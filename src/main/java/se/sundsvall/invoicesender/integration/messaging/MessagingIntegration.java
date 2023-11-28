package se.sundsvall.invoicesender.integration.messaging;

import static generated.se.sundsvall.messaging.Details.AccountTypeEnum.BANKGIRO;
import static generated.se.sundsvall.messaging.Details.PaymentReferenceTypeEnum.SE_OCR;
import static generated.se.sundsvall.messaging.DigitalInvoiceFile.ContentTypeEnum.APPLICATION_PDF;
import static generated.se.sundsvall.messaging.DigitalInvoiceRequest.TypeEnum.INVOICE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static se.sundsvall.invoicesender.model.Status.NOT_SENT;
import static se.sundsvall.invoicesender.model.Status.SENT;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.invoicesender.model.Item;
import se.sundsvall.invoicesender.model.Status;

import generated.se.sundsvall.messaging.Details;
import generated.se.sundsvall.messaging.DigitalInvoiceFile;
import generated.se.sundsvall.messaging.DigitalInvoiceParty;
import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.MessageStatus;

@Component
public class MessagingIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingIntegration.class);

    static final String INTEGRATION_NAME = "Messaging";

    private final MessagingClient client;
    private final String invoiceSubject;
    private final String invoiceReferencePrefix;

    MessagingIntegration(final MessagingClient client, final MessagingIntegrationProperties properties) {
        this.client = client;

        invoiceSubject = properties.invoiceSubject();
        invoiceReferencePrefix = properties.invoiceReferencePrefix();
    }

    public Status sendInvoice(final String path, final Item invoice) {
        try {
            var invoiceContent = Files.readAllBytes(Paths.get(path).resolve(invoice.getFilename()));
            var encodedInvoiceContent = new String(Base64.getEncoder().encode(invoiceContent), UTF_8);

            var request = new DigitalInvoiceRequest()
                .type(INVOICE)
                .subject(invoiceSubject)
                .party(new DigitalInvoiceParty().partyId(UUID.fromString(invoice.getRecipientPartyId())))
                .reference(invoiceReferencePrefix + invoice.getMetadata().getInvoiceNumber())
                .details(new Details()
                    .amount(Float.valueOf(invoice.getMetadata().getTotalAmount()))
                    .dueDate(LocalDate.parse(invoice.getMetadata().getDueDate()))
                    .paymentReferenceType(SE_OCR)
                    .paymentReference(invoice.getMetadata().getPaymentReference())
                    .accountType(BANKGIRO)
                    .accountNumber(invoice.getMetadata().getAccountNumber()))
                .files(List.of(new DigitalInvoiceFile()
                    .filename(invoice.getFilename())
                    .contentType(APPLICATION_PDF)
                    .content(encodedInvoiceContent)));

            var response = client.sendDigitalInvoice(request);

            // We know that we have a single message with a single delivery - extract the status
            var status = response.getMessages().get(0).getDeliveries().get(0).getStatus();

            return status == MessageStatus.SENT ? SENT : NOT_SENT;
        } catch (Exception e) {
            LOG.warn("Unable to send invoice", e);

            return NOT_SENT;
        }
    }
}
