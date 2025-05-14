package se.sundsvall.invoicesender.service;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.IN_PROGRESS;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.METADATA_INCOMPLETE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_LEGAL_ID_FOUND;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_PARTY_ID_FOUND;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_PARTY_ID_NOT_FOUND;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.OTHER;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.INVOICE_COULD_NOT_BE_SENT;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.ITEM_IS_A_PDF;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.ITEM_IS_NOT_PROCESSABLE;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.RECIPIENT_HAS_INVALID_LEGAL_ID;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.RECIPIENT_HAS_INVALID_PARTY_ID;
import static se.sundsvall.invoicesender.service.util.CronUtil.parseCronExpression;
import static se.sundsvall.invoicesender.util.Constants.BATCH_FILE_SUFFIX;
import static se.sundsvall.invoicesender.util.Constants.DISABLED_CRON;
import static se.sundsvall.invoicesender.util.Constants.RECIPIENT_PATTERN;
import static se.sundsvall.invoicesender.util.Constants.X_PATH_FILENAME_EXPRESSION;
import static se.sundsvall.invoicesender.util.LegalIdUtil.isValidLegalId;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.invoicesender.integration.citizen.CitizenIntegration;
import se.sundsvall.invoicesender.integration.db.DbIntegration;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;
import se.sundsvall.invoicesender.integration.messaging.MessagingIntegration;
import se.sundsvall.invoicesender.integration.party.PartyIntegration;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegration;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegrationProperties;
import se.sundsvall.invoicesender.service.model.Metadata;
import se.sundsvall.invoicesender.service.util.XmlUtil;

@Service
public class InvoiceProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(InvoiceProcessor.class);
	private static final String SLACK_ERROR_MESSAGE = "Fatal error occured when processing invoices. Error message: '%s'. Search ELK with log id %s for more information.";
	private static final String ARCHIVE_INDEX = "ArchiveIndex.xml";
	private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>";

	private final FileSystem fileSystem;
	private final CitizenIntegration citizenIntegration;
	private final PartyIntegration partyIntegration;
	private final MessagingIntegration messagingIntegration;
	private final DbIntegration dbIntegration;

	private final Map<String, RaindanceIntegration> raindanceIntegrations = new HashMap<>();
	private final Map<String, List<String>> invoiceFilenamePrefixes = new HashMap<>();

	public InvoiceProcessor(final FileSystem fileSystem, final TaskScheduler taskScheduler,
		final RaindanceIntegrationProperties properties,
		final CitizenIntegration citizenIntegration,
		final PartyIntegration partyIntegration,
		final MessagingIntegration messagingIntegration,
		final DbIntegration dbIntegration) {
		this.fileSystem = fileSystem;
		this.citizenIntegration = citizenIntegration;
		this.partyIntegration = partyIntegration;
		this.messagingIntegration = messagingIntegration;
		this.dbIntegration = dbIntegration;

		properties.environments().forEach((municipalityId, raindanceEnvironment) -> {
			// Create a Raindance integration for the given municipality id
			raindanceIntegrations.put(municipalityId, new RaindanceIntegration(raindanceEnvironment, fileSystem));

			// Get the invoice filename prefixes
			invoiceFilenamePrefixes.put(municipalityId, raindanceEnvironment.invoiceFilenamePrefixes());

			raindanceEnvironment.batchSetup().forEach((batchName, batchSetup) -> {
				final var cronExpression = batchSetup.scheduling().cronExpression();

				// Check if the batch is disabled
				if (DISABLED_CRON.equals(cronExpression)) {
					LOG.info("Batch with prefix {} is disabled", batchName);
					return;
				}

				final var parsedCronExpression = parseCronExpression(cronExpression);
				LOG.info("Scheduling run for batches with prefix {} {}", batchName, parsedCronExpression);

				// Create the cron trigger
				final var cronTrigger = new CronTrigger(cronExpression);
				// Schedule it
				taskScheduler.schedule(() -> {
					executeBatch(LocalDate.now(), municipalityId, batchName);
				}, cronTrigger);
			});
		});
	}

	/**
	 * Runs the invoice processor for the given date, municipality id and each configured batch-setup.
	 *
	 * @param date           the date.
	 * @param municipalityId the municipality id.
	 */
	public void run(final LocalDate date, final String municipalityId) {
		raindanceIntegrations.get(municipalityId).getBatchSetups()
			.forEach(batchName -> {
				executeBatch(date, municipalityId, batchName);
			});
	}

	private void executeBatch(final LocalDate date, final String municipalityId, String batchName) {
		try {
			LOG.info("Started process of batch with prefix {} for municipality {}", batchName, municipalityId);
			run(date, municipalityId, batchName);
			LOG.info("Ended process of batch with prefix {} for municipality {}", batchName, municipalityId);

		} catch (final Exception e) {
			LOG.error("Failed to process batch with prefix {} for municipality {}", batchName, municipalityId, e);

			// Display error on slack and send error report to mail
			messagingIntegration.sendSlackMessage(municipalityId, SLACK_ERROR_MESSAGE.formatted(e.getMessage(), RequestId.get()));
			messagingIntegration.sendErrorReport(date, municipalityId, batchName, e.getMessage());
		}
	}

	void run(final LocalDate date, final String municipalityId, final String batchName) throws IOException {
		// Get the Raindance integration
		final var raindanceIntegration = raindanceIntegrations.get(municipalityId);
		// Get the batches from Raindance
		final var batches = raindanceIntegration.readBatches(date, batchName, municipalityId);

		final var batchEntities = dbIntegration.persistBatches(batches);

		for (final var batchEntity : batchEntities) {
			if (batchEntity.isProcessingEnabled()) {
				LOG.info("Processing batch {}", batchEntity.getBasename() + BATCH_FILE_SUFFIX);
				final var localPath = batchEntity.getLocalPath();
				var archiveIndex = mapXmlFileToString(localPath);
				for (final var item : batchEntity.getItems()) {

					// Mark invoice items
					markItems(item, municipalityId);
					if (ITEM_IS_NOT_PROCESSABLE.test(item)) {
						// Stop processing item if it is not processable.
						LOG.info("Item not processable - skipping item {}", item.getFilename());
						dbIntegration.persistItem(item);
						continue;
					}

					// Extract the item metadata
					extractItemMetadata(item, archiveIndex);
					if (ITEM_IS_NOT_PROCESSABLE.test(item)) {
						// Stop processing item if it is not processable.
						LOG.info("Item not processable after extracting metadata - skipping item {}", item.getFilename());
						dbIntegration.persistItem(item);
						continue;
					}

					// Extract recipient legal id:s if possible
					extractInvoiceRecipientLegalId(item);
					if (RECIPIENT_HAS_INVALID_LEGAL_ID.test(item)) {
						// Stop processing item if it does not have a legal id.
						LOG.info("Item has an invalid legal id - skipping item {}", item.getFilename());
						dbIntegration.persistItem(item);
						continue;
					}

					// Remove any items that have invalid recipient legal ids
					validateLegalId(item);
					if (RECIPIENT_HAS_INVALID_LEGAL_ID.test(item)) {
						// Stop processing item if it has an invalid legal id.
						LOG.info("Invalid recipient legal id - skipping item {}", item.getFilename());
						dbIntegration.persistItem(item);
						continue;
					}

					// Get the recipient party id from the invoices that are left and where the recipient legal id is set
					fetchInvoiceRecipientPartyIds(item, municipalityId);
					if (RECIPIENT_HAS_INVALID_PARTY_ID.test(item)) {
						// Stop processing item if the recipient party id is invalid.
						LOG.info("Invalid recipient party id - skipping item {}", item.getFilename());
						dbIntegration.persistItem(item);
						continue;
					}

					// Remove any items where the recipient has a protected identity
					markProtectedIdentityItems(item, municipalityId);
					if (RECIPIENT_HAS_INVALID_LEGAL_ID.test(item)) {
						// Stop processing item if the recipient has a protected identity.
						LOG.info("Recipient has protected identity - skipping item {}", item.getFilename());
						dbIntegration.persistItem(item);
						continue;
					}

					// Send digital mail for the invoices where the recipient party id is set
					sendDigitalInvoices(item, localPath, municipalityId);
					if (INVOICE_COULD_NOT_BE_SENT.test(item)) {
						// Stop processing item if the invoice could not be sent.
						LOG.info("Invoice could not be sent - skipping item {}", item.getFilename());
						dbIntegration.persistItem(item);
						continue;
					}

					dbIntegration.persistItem(item);
					// Update the archive index - ArchiveIndex.xml
					archiveIndex = removeItemFromArchiveIndex(item, archiveIndex, localPath);
				}
			} else {
				LOG.info("Batch processing is disabled for {}", batchEntity.getBasename() + BATCH_FILE_SUFFIX);
			}

			// Write the batch back to Raindance
			raindanceIntegration.writeBatch(batchEntity);
			// Mark the batch as completed and store it
			updateAndPersistBatch(batchEntity);

			// Archive the batch
			if (isNotBlank(batchEntity.getArchivePath())) {
				LOG.info("Archiving batch {}", batchEntity.getBasename() + BATCH_FILE_SUFFIX);
				raindanceIntegration.archiveOriginalBatch(batchEntity);
			}
			// Clean up
			FileSystemUtils.deleteRecursively(fileSystem.getPath(batchEntity.getLocalPath()));
			messagingIntegration.sendSlackMessage(batchEntity, date, municipalityId);
		}
		// Send a status report
		messagingIntegration.sendStatusReport(batchEntities, date, municipalityId);
	}

	/**
	 * Mark items as either INVOICE or OTHER and set the status to IN_PROGRESS or IGNORED.
	 *
	 * @param item           the item to mark
	 * @param municipalityId the municipality id
	 */
	void markItems(final ItemEntity item, final String municipalityId) {
		if (ITEM_IS_A_PDF.test(item)) {
			LOG.info("Setting item {} type to INVOICE", item.getFilename());
			item.setType(INVOICE);
			item.setStatus(IN_PROGRESS);

			final var invoiceFilenamePrefixesForMunicipality = invoiceFilenamePrefixes.get(municipalityId);

			if (isNotEmpty(invoiceFilenamePrefixesForMunicipality) && invoiceFilenamePrefixesForMunicipality.stream().noneMatch(prefix -> item.getFilename().startsWith(prefix))) {
				LOG.info("Setting item {} status to IGNORED", item.getFilename());
				item.setStatus(IGNORED);
			}
		} else {
			LOG.info("Setting item {} type to OTHER and status to IGNORED", item.getFilename());
			item.setType(OTHER);
			item.setStatus(IGNORED);
		}
	}

	/**
	 * Maps the XML file to a string. The XML file is read from the local path and returned as a string.
	 *
	 * @param  localPath   the local path to the file
	 * @return             the XML file as a string
	 * @throws IOException if an I/O error occurs
	 */
	String mapXmlFileToString(final String localPath) throws IOException {
		final var path = fileSystem.getPath(localPath).resolve(ARCHIVE_INDEX);
		return Files.readString(path, ISO_8859_1);
	}

	/**
	 * Extracts the recipient legal id from the invoice PDF filename and updates the invoice accordingly.
	 *
	 * @param item the item to extract the legal id from
	 */
	void extractInvoiceRecipientLegalId(final ItemEntity item) {
		// Try to extract the recipient's legal id from the invoice PDF filename and update
		// the invoice accordingly
		final var matcher = RECIPIENT_PATTERN.matcher(item.getFilename());
		if (matcher.matches()) {
			LOG.info("Extracted recipient legal id for item {}", item.getFilename());
			item.setStatus(RECIPIENT_LEGAL_ID_FOUND);
			item.setRecipientLegalId(matcher.group(1));
		} else {
			LOG.info("Failed to extract recipient legal id for item {}", item.getFilename());
			item.setStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
		}
	}

	/**
	 * Removes the item from the archive index XML file. The XML file is updated and written back to the local path.
	 *
	 * @param  item            the item to remove
	 * @param  archiveIndexXml the archive index XML file as a string
	 * @param  localPath       the local path to the file
	 * @return                 the updated XML file as a string
	 * @throws IOException     if an I/O error occurs
	 */
	String removeItemFromArchiveIndex(final ItemEntity item, final String archiveIndexXml, final String localPath) throws IOException {
		final var newXml = XmlUtil.remove(archiveIndexXml, X_PATH_FILENAME_EXPRESSION.formatted(item.getFilename()));
		final var path = fileSystem.getPath(localPath).resolve(ARCHIVE_INDEX);
		Files.writeString(path, XML_DECLARATION.concat("\n").concat(newXml), ISO_8859_1);
		LOG.info("Removed item {} from ArchiveIndex.xml", item.getFilename());
		return newXml;
	}

	/**
	 * Extracts metadata from the archive index for the given item. The metadata is extracted from the XML file and set on
	 * the item.
	 *
	 * @param item         the item to extract metadata for
	 * @param archiveIndex the archive index XML file as a string
	 */
	void extractItemMetadata(final ItemEntity item, final String archiveIndex) {
		LOG.info("Extracting metadata for item {}", item.getFilename());
		var xpathExpression = X_PATH_FILENAME_EXPRESSION.formatted(item.getFilename());
		LOG.info("Xpath expression: {}", xpathExpression);

		// Evaluate
		final var result = XmlUtil.find(archiveIndex, xpathExpression);

		// Extract the item metadata
		final var invoiceNumber = XmlUtil.getChildNodeText(result, "InvoiceNo");
		final var invoiceDate = XmlUtil.getChildNodeText(result, "InvoiceDate");
		final var dueDate = XmlUtil.getChildNodeText(result, "DueDate");
		final var payable = !"01".equals(XmlUtil.getChildNodeText(result, "AGF"));
		final var reminder = "1".equals(XmlUtil.getChildNodeText(result, "Reminder"));
		final var accountNumber = XmlUtil.getChildNodeText(result, "PaymentNo");
		final var paymentReference = XmlUtil.getChildNodeText(result, "PaymentReference");
		final var totalAmount = XmlUtil.getChildNodeText(result, "TotalAmount");

		// Check if we've managed to extract all required metadata fields. If not - mark it as incomplete and
		// bail out early since we won't do any further processing on the item
		if (isAnyBlank(invoiceNumber, invoiceDate, dueDate, accountNumber, paymentReference, totalAmount)) {
			item.setStatus(METADATA_INCOMPLETE);
			return;
		}

		// Set the item metadata
		item.setMetadata(new Metadata()
			.withInvoiceNumber(invoiceNumber)
			.withInvoiceDate(invoiceDate)
			.withDueDate(dueDate)
			.withPayable(payable)
			.withReminder(reminder)
			.withAccountNumber(accountNumber)
			.withPaymentReference(paymentReference)
			.withTotalAmount(totalAmount));
	}

	/**
	 * Validates that the legal id is 10 digits and also validates that the check digit is correct. If the validation fails,
	 * we set a status to indicate the failure.
	 *
	 * @param item the item to check
	 */
	void validateLegalId(final ItemEntity item) {
		if (!isValidLegalId(item.getRecipientLegalId())) {
			item.setStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
		}
	}

	/**
	 * Mark items with protected identities as invalid.
	 *
	 * @param item           the item to check
	 * @param municipalityId the municipality id
	 */
	void markProtectedIdentityItems(final ItemEntity item, final String municipalityId) {
		if (citizenIntegration.hasProtectedIdentity(item.getRecipientPartyId(), municipalityId)) {
			item.setStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
		}
	}

	/**
	 * Fetch the recipient party id from the party integration. Tries to set the party id on the item, sets a status which
	 * indicates success or failure.
	 *
	 * @param item           the item to check
	 * @param municipalityId the municipality id
	 */
	void fetchInvoiceRecipientPartyIds(final ItemEntity item, final String municipalityId) {
		partyIntegration.getPartyId(item.getRecipientLegalId(), municipalityId)
			.ifPresentOrElse(legalIdAndPartyId -> {
				LOG.info("Fetched recipient party id for item {}", item.getFilename());

				item.setRecipientLegalId(legalIdAndPartyId.legalId());
				item.setRecipientPartyId(legalIdAndPartyId.partyId());
				item.setStatus(RECIPIENT_PARTY_ID_FOUND);
			}, () -> {
				LOG.info("Failed to fetch recipient party id for item {}", item.getFilename());

				item.setStatus(RECIPIENT_PARTY_ID_NOT_FOUND);
			});
	}

	/**
	 * Tries to send the invoice with messaging. Sets a status which indicates success or failure.
	 *
	 * @param item           the item to send
	 * @param localPath      the local path to the file
	 * @param municipalityId the municipality id
	 */
	void sendDigitalInvoices(final ItemEntity item, final String localPath, final String municipalityId) {
		final var status = messagingIntegration.sendInvoice(localPath, item, municipalityId);
		item.setStatus(status);
		LOG.info("{} invoice {}", status == SENT ? "Sent" : "Couldn't send", item.getFilename());

	}

	void updateAndPersistBatch(final BatchEntity batchEntity) {
		batchEntity.setCompleted(true);
		batchEntity.setCompletedAt(LocalDateTime.now());

		batchEntity.setIgnoredItems(batchEntity.getItems().stream()
			.filter(item -> item.getStatus() == IGNORED)
			.count());
		batchEntity.setSentItems(batchEntity.getItems().stream()
			.filter(item -> item.getStatus() == SENT)
			.count());

		dbIntegration.persistBatch(batchEntity);
	}

}
