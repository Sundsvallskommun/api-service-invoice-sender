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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.function.Failable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import se.sundsvall.invoicesender.integration.db.DbIntegration;
import se.sundsvall.invoicesender.integration.messaging.MessagingIntegration;
import se.sundsvall.invoicesender.integration.party.PartyIntegration;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegration;
import se.sundsvall.invoicesender.model.Batch;
import se.sundsvall.invoicesender.model.Item;

import us.codecraft.xsoup.Xsoup;

@Service
public class InvoiceSenderService {

    private static final Pattern RECIPIENT_PATTERN = Pattern.compile("^.*aktura_\\d+_to_(\\d+)\\.pdf$");

    private static final Predicate<Item> PDFS_ONLY = invoice -> invoice.getFilename().endsWith(".pdf");
    private static final Predicate<Item> INVOICES_WITH_LEGAL_ID = invoice -> invoice.getStatus() == RECIPIENT_LEGAL_ID_FOUND;
    private static final Predicate<Item> INVOICES_WITH_PARTY_ID = invoice -> invoice.getStatus() == RECIPIENT_PARTY_ID_FOUND;

    private final RaindanceIntegration raindanceIntegration;
    private final PartyIntegration partyIntegration;
    private final MessagingIntegration messagingIntegration;
    private final DbIntegration dbIntegration;

    public InvoiceSenderService(final RaindanceIntegration raindanceIntegration,
            final PartyIntegration partyIntegration, final MessagingIntegration messagingIntegration,
            final DbIntegration dbIntegration) {
        this.raindanceIntegration = raindanceIntegration;
        this.partyIntegration = partyIntegration;
        this.messagingIntegration = messagingIntegration;
        this.dbIntegration = dbIntegration;
    }

    // TODO: this should be scheduled later on...
    public void doStuff() throws IOException {
        // Get the batch from Raindance
        Failable.stream(raindanceIntegration.readBatch()).forEach(batch -> {
            // Mark non-PDF files
            batch.getItems().stream()
                .filter(not(PDFS_ONLY))
                .forEach(invoice -> invoice.setStatus(NOT_AN_INVOICE));
            // Extract individual files (file names), skipping everything but PDF:s
            var invoices = batch.getItems().stream()
                .filter(PDFS_ONLY)
                .toList();

            // Extract recipient legal id:s if possible
            extractInvoiceRecipientLegalIds(invoices);
            // Get the recipient party id from the invoices where the recipient legal id is set
            getInvoiceRecipientPartyIds(invoices);
            // Send digital mail for the invoices where the recipient party id is set
            sendDigitalInvoices(batch.getPath(), invoices);
            // Update the archive index - ArchiveIndex.xml
            updateArchiveIndex(batch.getPath(), invoices);
            // Put unsent invoices back to Raindance
            raindanceIntegration.writeBatch(batch);
            // Mark the batch as completed and store it
            completeBatchAndStoreExecution(batch);
            // Cleanup
            FileUtils.deleteDirectory(Paths.get(batch.getPath()).toFile());
        });
    }

    void extractInvoiceRecipientLegalIds(final List<Item> invoices) {
        invoices.forEach(invoice -> {
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

    void getInvoiceRecipientPartyIds(final List<Item> invoices) {
        invoices.stream()
            .filter(INVOICES_WITH_LEGAL_ID)
            .forEach(invoice -> partyIntegration.getPartyId(invoice.getRecipientLegalId())
                .ifPresentOrElse(partyId -> {
                    invoice.setRecipientPartyId(partyId);
                    invoice.setStatus(RECIPIENT_PARTY_ID_FOUND);
                }, () -> invoice.setStatus(RECIPIENT_PARTY_ID_NOT_FOUND)));
    }

    void sendDigitalInvoices(final String path, final List<Item> invoices) {
        invoices.stream()
            .filter(INVOICES_WITH_PARTY_ID)
            .forEach(invoice -> invoice.setStatus(messagingIntegration.sendInvoice(path, invoice)));
    }

    void updateArchiveIndex(final String path, final List<Item> items) {
        items.stream()
            .filter(item -> "ArchiveIndex.xml".equalsIgnoreCase(item.getFilename()))
            .findFirst()
            .ifPresent(archiveIndexItem -> Failable.accept(item -> {
                var archiveIndexFile = new File(path + File.pathSeparator + archiveIndexItem.getFilename());
                // Create the XPath expression from the filenames of the files that have been sent
                var xPathExpression = items.stream()
                    .filter(currentItem -> currentItem.getStatus() == SENT)
                    .map(Item::getFilename)
                    .map(filename -> format("filename='%s'", filename))
                    .collect(joining(" OR ", "//file[", "]"));
                // Remove the matching nodes
                var doc = Jsoup.parse(archiveIndexFile);
                var result = Xsoup.compile(xPathExpression).evaluate(doc);
                result.getElements().forEach(Element::remove);

                FileUtils.write(archiveIndexFile, doc.outerHtml(), ISO_8859_1);
            }, archiveIndexItem));
    }

    void completeBatchAndStoreExecution(final Batch batch) {
        // Mark the batch as completed
        batch.setCompleted();
        // Store the batch execution
        dbIntegration.storeBatchExecution(batch);
    }
}
