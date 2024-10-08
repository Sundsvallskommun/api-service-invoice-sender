package se.sundsvall.invoicesender.service;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.function.Predicate.not;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static se.sundsvall.invoicesender.model.Item.ITEM_HAS_LEGAL_ID;
import static se.sundsvall.invoicesender.model.Item.ITEM_HAS_PARTY_ID;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_A_PDF;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_PROCESSABLE;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_SENT;
import static se.sundsvall.invoicesender.model.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.model.ItemStatus.METADATA_INCOMPLETE;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_LEGAL_ID_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_PARTY_ID_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_PARTY_ID_NOT_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;
import static se.sundsvall.invoicesender.model.ItemType.INVOICE;
import static se.sundsvall.invoicesender.model.ItemType.OTHER;
import static se.sundsvall.invoicesender.service.util.CronUtil.parseCronExpression;
import static se.sundsvall.invoicesender.util.LegalIdUtil.isValidLegalId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.sundsvall.invoicesender.integration.citizen.CitizenIntegration;
import se.sundsvall.invoicesender.integration.db.DbIntegration;
import se.sundsvall.invoicesender.integration.db.dto.BatchDto;
import se.sundsvall.invoicesender.integration.messaging.MessagingIntegration;
import se.sundsvall.invoicesender.integration.party.PartyIntegration;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegration;
import se.sundsvall.invoicesender.model.Batch;
import se.sundsvall.invoicesender.model.Item;
import se.sundsvall.invoicesender.service.util.XmlUtil;

@Service
public class InvoiceProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(InvoiceProcessor.class);

	private static final Pattern RECIPIENT_PATTERN = Pattern.compile("\\w+_\\d+_to_(\\d+)\\.pdf$");

	private static final String BATCH_FILE_SUFFIX = ".zip.7z";

	private final RaindanceIntegration raindanceIntegration;
	private final CitizenIntegration citizenIntegration;
	private final PartyIntegration partyIntegration;

	private final MessagingIntegration messagingIntegration;

	private final DbIntegration dbIntegration;

	private final List<String> invoiceFilenamePrefixes;
	private final List<String> municipalityIds;

	public InvoiceProcessor(final InvoiceProcessorProperties properties,
			final RaindanceIntegration raindanceIntegration,
			final CitizenIntegration citizenIntegration,
			final PartyIntegration partyIntegration,
			final MessagingIntegration messagingIntegration,
			final DbIntegration dbIntegration) {
		this.raindanceIntegration = raindanceIntegration;
		this.citizenIntegration = citizenIntegration;
		this.partyIntegration = partyIntegration;
		this.messagingIntegration = messagingIntegration;
		this.dbIntegration = dbIntegration;

		invoiceFilenamePrefixes = properties.invoiceFilenamePrefixes();
		municipalityIds = properties.schedule().municipalityIds();

		var cronExpression = properties.schedule().cronExpression();
		if (!"-".equals(cronExpression)) {
			var parsedCronExpression = parseCronExpression(cronExpression);
			LOG.info("Invoice processor is ENABLED to run {}", parsedCronExpression);
		} else {
			LOG.info("Invoice processor scheduling is DISABLED");
		}
	}

	@Scheduled(cron = "${invoice-processor.schedule.cron-expression:-}")
	void run() {
		municipalityIds.forEach(municipalityId -> {
			try {
				run(LocalDate.now(), municipalityId);
			} catch (final Exception e) {
				LOG.error("Failed to process invoices for municipality {}", municipalityId, e);
			}
		});
	}

	public void run(final LocalDate date, final String municipalityId) throws IOException {
		// Get the batches from Raindance
		var batches = raindanceIntegration.readBatches(date);

		var processedBatches = new ArrayList<BatchDto>();

		for (var batch : batches) {
			if (batch.isProcessingEnabled()) {
				LOG.info("Processing batch {}", batch.getBasename() + BATCH_FILE_SUFFIX);

				// Mark non-invoice items (i.e. not PDF:s)
				markNonPdfItemsAsOther(batch);
				// Mark invoice items
				markInvoiceItems(batch);

				// Extract the item metadata
				extractItemMetadata(batch);
				// Extract recipient legal id:s if possible
				extractInvoiceRecipientLegalIds(batch);
				// Remove any items that have invalid recipient legal ids
				markItemsWithInvalidLegalIds(batch);
				// Remove any items where the recipient has a protected identity
				markProtectedIdentityItems(batch);
				// Get the recipient party id from the invoices that are left and where the recipient legal id is set
				fetchInvoiceRecipientPartyIds(batch, municipalityId);
				// Send digital mail for the invoices where the recipient party id is set
				sendDigitalInvoices(batch, municipalityId);
				// Update the archive index - ArchiveIndex.xml
				updateArchiveIndex(batch);
			} else {
				LOG.info("Batch processing is disabled for {}", batch.getBasename() + BATCH_FILE_SUFFIX);
			}

			// Write the batch back to Raindance
			raindanceIntegration.writeBatch(batch);
			// Mark the batch as completed and store it
			processedBatches.add(completeBatchAndStoreExecution(batch, municipalityId));

			// Archive the batch
			if (isNotBlank(batch.getArchivePath())) {
				LOG.info("Archiving batch {}", batch.getBasename() + BATCH_FILE_SUFFIX);

				raindanceIntegration.archiveOriginalBatch(batch);
			}

			// Clean up
			FileUtils.deleteDirectory(Paths.get(batch.getLocalPath()).toFile());
		}

		// Send a status report
		messagingIntegration.sendStatusReport(processedBatches, municipalityId);
	}

	/**
	 * Marks the items in the batch that aren't PDF files as "other".
	 *
	 * @param batch the batch.
	 */
	void markNonPdfItemsAsOther(final Batch batch) {
		getItems(batch, not(ITEM_IS_A_PDF)).forEach(item -> {
			LOG.info("Marking item {} as OTHER", item.getFilename());

			item.setType(OTHER);
		});
	}

	/**
	 * Marks the items in the batch that are PDF files as invoices. Also, if invoice filename
	 * prefixes is set and non-empty, marks any items that don't start with any of the prefixes as
	 * ignored.
	 *
	 * @param batch the batch.
	 */
	void markInvoiceItems(final Batch batch) {
		getItems(batch, ITEM_IS_A_PDF).forEach(item -> {
			LOG.info("Marking item {} as an INVOICE", item.getFilename());

			item.setType(INVOICE);

			if (isNotEmpty(invoiceFilenamePrefixes) && invoiceFilenamePrefixes.stream().noneMatch(prefix -> item.getFilename().startsWith(prefix))) {
				LOG.info("Marking item {} as IGNORED", item.getFilename());

				item.setStatus(IGNORED);
			}
		});
	}

	void extractItemMetadata(final Batch batch) throws IOException {
		var path = Paths.get(batch.getLocalPath()).resolve("ArchiveIndex.xml");
		var xml = Files.readString(path, ISO_8859_1);

		getProcessableInvoiceItems(batch).forEach(item -> {
			LOG.info("Extracting metadata for item {}", item.getFilename());

			// Create the XPath expression from the item filename
			var xPathExpression = format("//file[filename='%s']", item.getFilename());
			// Evaluate
			var result = XmlUtil.find(xml, xPathExpression);
			// Extract the item metadata
			var invoiceNumber = result.select("InvoiceNo").text();
			var invoiceDate = result.select("InvoiceDate").text();
			var dueDate = result.select("DueDate").text();
			var payable = !"01".equals(result.select("AGF").text().trim());
			var reminder = "1".equals(result.select("Reminder").text());
			var accountNumber = result.select("PaymentNo").text();
			var paymentReference = result.select("PaymentReference").text();
			var totalAmount = result.select("TotalAmount").text();

			// Check if we've managed to extract all required metadata fields. If not - mark it as incomplete and
			// bail out early since we won't do any further processing on the item
			if (isAnyBlank(invoiceNumber, invoiceDate, dueDate, accountNumber, paymentReference, totalAmount)) {
				item.setStatus(METADATA_INCOMPLETE);

				return;
			}

			// Set the item metadata
			item.setMetadata(new Item.Metadata()
				.withInvoiceNumber(invoiceNumber)
				.withInvoiceDate(invoiceDate)
				.withDueDate(dueDate)
				.withPayable(payable)
				.withReminder(reminder)
				.withAccountNumber(accountNumber)
				.withPaymentReference(paymentReference)
				.withTotalAmount(totalAmount)
			);
		});
	}

	void extractInvoiceRecipientLegalIds(final Batch batch) {
		getProcessableInvoiceItems(batch).forEach(item -> {
			// Try to extract the recipient's legal id from the invoice PDF filename and update
			// the invoice accordingly
			var matcher = RECIPIENT_PATTERN.matcher(item.getFilename());
			if (matcher.matches()) {
				LOG.info("Extracted recipient legal id for item {}", item.getFilename());

				item.setStatus(RECIPIENT_LEGAL_ID_FOUND);
				item.setRecipientLegalId(matcher.group(1));
			} else {
				LOG.info("Failed to extract recipient legal id for item {}", item.getFilename());

				item.setStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
			}
		});
	}

	void markItemsWithInvalidLegalIds(final Batch batch) {
		getInvoiceItemsWithLegalIdSet(batch).forEach(item -> {
			if (!isValidLegalId(item.getRecipientLegalId())) {
				LOG.info("Invalid recipient legal id - skipping item {}", item.getFilename());

				item.setStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
			}
		});
	}

	void markProtectedIdentityItems(final Batch batch) {
		getInvoiceItemsWithLegalIdSet(batch).forEach(item -> {
			if (citizenIntegration.hasProtectedIdentity(item.getRecipientLegalId())) {
				LOG.info("Recipient has protected identity - skipping item {}", item.getFilename());

				item.setStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
			}
		});
	}

	void fetchInvoiceRecipientPartyIds(final Batch batch, final String municipalityId) {
		getInvoiceItemsWithLegalIdSet(batch).forEach(item ->
			partyIntegration.getPartyId(item.getRecipientLegalId(), municipalityId)
				.ifPresentOrElse(partyId -> {
					LOG.info("Fetched recipient party id for item {}", item.getFilename());

					item.setRecipientPartyId(partyId);
					item.setStatus(RECIPIENT_PARTY_ID_FOUND);
				}, () -> {
					LOG.info("Failed to fetch recipient party id for item {}", item.getFilename());

					item.setStatus(RECIPIENT_PARTY_ID_NOT_FOUND);
				}));
	}

	void sendDigitalInvoices(final Batch batch, final String municipalityId) {
		getInvoiceItemsWithPartyIdSet(batch).forEach(item -> {
			var status = messagingIntegration.sendInvoice(batch.getLocalPath(), item, municipalityId);

			item.setStatus(status);

			LOG.info("{} invoice {}", status == SENT ? "Sent" : "Couldn't send", item.getFilename());
		});
	}

	void updateArchiveIndex(final Batch batch) throws IOException {
		var sentItems = getSentInvoiceItems(batch);

		if (!sentItems.isEmpty()) {
			var path = Paths.get(batch.getLocalPath()).resolve("ArchiveIndex.xml");
			var xml = Files.readString(path, ISO_8859_1);

			for (var sentItem : sentItems) {
				var xPathExpression = String.format("//file[filename='%s']", sentItem.getFilename());

				// Remove the matching nodes
				xml = XmlUtil.remove(xml, xPathExpression);
			}

			Files.writeString(path, XmlUtil.XML_DECLARATION.concat("\n").concat(xml), ISO_8859_1);
		}
	}

	BatchDto completeBatchAndStoreExecution(final Batch batch, final String municipalityId) {
		// Mark the batch as completed
		batch.setCompleted();
		// Store the batch execution
		return dbIntegration.storeBatch(batch, municipalityId);
	}

	List<Item> getInvoiceItemsWithLegalIdSet(final Batch batch) {
		return getProcessableInvoiceItems(batch).stream()
			.filter(ITEM_HAS_LEGAL_ID)
			.toList();
	}

	List<Item> getInvoiceItemsWithPartyIdSet(final Batch batch) {
		return getProcessableInvoiceItems(batch).stream()
			.filter(ITEM_HAS_PARTY_ID)
			.toList();
	}

	List<Item> getSentInvoiceItems(final Batch batch) {
		return getProcessableInvoiceItems(batch).stream()
			.filter(ITEM_IS_SENT)
			.toList();
	}

	List<Item> getProcessableInvoiceItems(final Batch batch) {
		return getItems(batch, ITEM_IS_PROCESSABLE);
	}

	List<Item> getItems(final Batch batch, final Predicate<Item> predicate) {
		return batch.getItems().stream()
			.filter(predicate)
			.toList();
	}
}
