package se.sundsvall.invoicesender.service;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static se.sundsvall.invoicesender.model.Status.NOT_AN_INVOICE;
import static se.sundsvall.invoicesender.model.Status.RECIPIENT_LEGAL_ID_FOUND;
import static se.sundsvall.invoicesender.model.Status.RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID;
import static se.sundsvall.invoicesender.model.Status.RECIPIENT_PARTY_ID_FOUND;
import static se.sundsvall.invoicesender.model.Status.RECIPIENT_PARTY_ID_NOT_FOUND;
import static se.sundsvall.invoicesender.model.Status.SENT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    private static final Pattern RECIPIENT_PATTERN = Pattern.compile("^Faktura_\\d+_to_(\\d+)\\.pdf$");

    private static final Predicate<Item> PDFS_ONLY = invoice -> invoice.getFilename().endsWith(".pdf");
    private static final Predicate<Item> INVOICES_WITH_LEGAL_ID = invoice -> invoice.getStatus() == RECIPIENT_LEGAL_ID_FOUND;
    private static final Predicate<Item> INVOICES_WITH_PARTY_ID = invoice -> invoice.getStatus() == RECIPIENT_PARTY_ID_FOUND;
    private static final Predicate<Item> SENT_INVOICES = invoice -> invoice.getStatus() == SENT;

    private final RaindanceIntegration raindanceIntegration;
    private final PartyIntegration partyIntegration;
    private final MessagingIntegration messagingIntegration;
    private final DbIntegration dbIntegration;

    public InvoiceProcessor(final RaindanceIntegration raindanceIntegration,
            final PartyIntegration partyIntegration, final MessagingIntegration messagingIntegration,
            final DbIntegration dbIntegration) {
        this.raindanceIntegration = raindanceIntegration;
        this.partyIntegration = partyIntegration;
        this.messagingIntegration = messagingIntegration;
        this.dbIntegration = dbIntegration;
    }

    @Scheduled(cron = "${invoice-processor.schedule.cron-expression:-}")
    void run() throws Exception {
        run(LocalDate.now());
    }

    public void run(final LocalDate date) throws Exception {
        // Get the batches from Raindance
        var batches = raindanceIntegration.readBatch(date);

        var processedBatches = new ArrayList<BatchDto>();
        for (var batch : batches) {
            // Mark non-invoice items (i.e. not PDF:s)
            getNonInvoiceItems(batch).forEach(item -> item.setStatus(NOT_AN_INVOICE));
            // Extract the item metadata
            extractItemMetadata(batch);
            // Extract recipient legal id:s if possible
            extractInvoiceRecipientLegalIds(batch);
            // Get the recipient party id from the invoices where the recipient legal id is set
            getInvoiceRecipientPartyIds(batch);
            // Send digital mail for the invoices where the recipient party id is set
            sendDigitalInvoices(batch);
            // Update the archive index - ArchiveIndex.xml
            updateArchiveIndex(batch);
            // Put unsent invoices back to Raindance
            raindanceIntegration.writeBatch(batch);
            // Mark the batch as completed and store it
            processedBatches.add(completeBatchAndStoreExecution(batch));
            // Clean up
            FileUtils.deleteDirectory(Paths.get(batch.getPath()).toFile());
        }

        // Send a status report
        messagingIntegration.sendStatusReport(processedBatches);
    }

    void extractItemMetadata(final Batch batch) throws IOException {
        var path = Paths.get(batch.getPath()).resolve("ArchiveIndex.xml");
        var xml = Files.readString(path, ISO_8859_1);

        getInvoiceItems(batch).forEach(item -> {
            // Create the XPath expression from the item filename
            var xPathExpression = format("//file[filename='%s']", item.getFilename());
            // Evaluate
            var result = XmlUtil.find(xml, xPathExpression);
            // Extract the item metadata
            item.setMetadata(new Item.Metadata()
                .setInvoiceNumber(result.select("InvoiceNo").text())
                .setInvoiceDate(result.select("InvoiceDate").text())
                .setDueDate(result.select("DueDate").text())
                .setReminder("1".equals(result.select("Reminder").text()))
                .setAccountNumber(result.select("PaymentNo").text())
                .setPaymentReference(result.select("PaymentReference").text())
                .setTotalAmount(result.select("TotalAmount").text())
            );
        });
    }

    void extractInvoiceRecipientLegalIds(final Batch batch) {
        getInvoiceItems(batch).forEach(invoice -> {
            // Try to extract the recipient's legal id from the invoice PDF filename and update
            // the invoice accordingly
            var matcher = RECIPIENT_PATTERN.matcher(invoice.getFilename());
            if (matcher.matches()) {
                invoice.setStatus(RECIPIENT_LEGAL_ID_FOUND);
                invoice.setRecipientLegalId(matcher.group(1));
            } else {
                invoice.setStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
            }
        });
    }

    void getInvoiceRecipientPartyIds(final Batch batch) {
        getInvoiceItemsWithLegalIdSet(batch).forEach(invoice ->
            partyIntegration.getPartyId(invoice.getRecipientLegalId())
                .ifPresentOrElse(partyId -> {
                    invoice.setRecipientPartyId(partyId);
                    invoice.setStatus(RECIPIENT_PARTY_ID_FOUND);
                }, () -> invoice.setStatus(RECIPIENT_PARTY_ID_NOT_FOUND)));
    }

    void sendDigitalInvoices(final Batch batch) {
        getInvoiceItemsWithPartyIdSet(batch).forEach(invoice ->
            invoice.setStatus(messagingIntegration.sendInvoice(batch.getPath(), invoice)));
    }

    void updateArchiveIndex(final Batch batch) throws IOException {
        var sentItems = getSentInvoiceItems(batch);

        if (!sentItems.isEmpty()) {
            var xPathExpression = sentItems.stream()
                .map(Item::getFilename)
                .map(filename -> format("filename='%s'", filename))
                .collect(joining(" OR ", "//file[", "]"));

            var path = Paths.get(batch.getPath()).resolve("ArchiveIndex.xml");
            var xml = Files.readString(path, ISO_8859_1);

            // Remove the matching nodes
            xml = XmlUtil.remove(xml, xPathExpression);

            Files.writeString(path, XmlUtil.XML_DECLARATION.concat("\n").concat(xml), ISO_8859_1);
        }
    }

    BatchDto completeBatchAndStoreExecution(final Batch batch) {
        // Mark the batch as completed
        batch.setCompleted();
        // Store the batch execution
        return dbIntegration.storeBatchExecution(batch);
    }

    List<Item> getInvoiceItems(final Batch batch) {
        return batch.getItems().stream()
            .filter(PDFS_ONLY)
            .toList();
    }

    List<Item> getNonInvoiceItems(final Batch batch) {
        return batch.getItems().stream()
            .filter(not(PDFS_ONLY))
            .toList();
    }

    List<Item> getInvoiceItemsWithLegalIdSet(final Batch batch) {
        return getInvoiceItems(batch).stream()
            .filter(INVOICES_WITH_LEGAL_ID)
            .toList();
    }

    List<Item> getInvoiceItemsWithPartyIdSet(final Batch batch) {
        return getInvoiceItems(batch).stream()
            .filter(INVOICES_WITH_PARTY_ID)
            .toList();
    }

    List<Item> getSentInvoiceItems(final Batch batch) {
        return getInvoiceItems(batch).stream()
            .filter(SENT_INVOICES)
            .toList();
    }
}
