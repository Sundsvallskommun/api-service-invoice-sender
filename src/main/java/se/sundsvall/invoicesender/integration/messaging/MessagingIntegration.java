package se.sundsvall.invoicesender.integration.messaging;

import static generated.se.sundsvall.messaging.DigitalMailAttachment.ContentTypeEnum.APPLICATION_PDF;
import static java.nio.charset.StandardCharsets.UTF_8;
import static se.sundsvall.invoicesender.model.Status.NOT_SENT;
import static se.sundsvall.invoicesender.model.Status.SENT;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import se.sundsvall.invoicesender.model.Item;
import se.sundsvall.invoicesender.model.Status;

import generated.se.sundsvall.messaging.DigitalMailAttachment;
import generated.se.sundsvall.messaging.DigitalMailParty;
import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.MessageStatus;

@Component
public class MessagingIntegration {

    static final String INTEGRATION_NAME = "Messaging";

    private final MessagingClient client;
    private final String invoiceSubject;

    MessagingIntegration(final MessagingClient client, final MessagingIntegrationProperties properties) {
        this.client = client;

        invoiceSubject = properties.invoiceSubject();
    }

    public Status sendInvoice(final String path, final Item invoice) {
        try {
            var invoiceContent = Files.readAllBytes(Paths.get(path).resolve(invoice.getFilename()));
            var encodedInvoiceContent = new String(Base64.getEncoder().encode(invoiceContent), UTF_8);

            var request = new DigitalMailRequest()
                .subject(invoiceSubject)
                .party(new DigitalMailParty().partyIds(List.of(UUID.fromString(invoice.getRecipientPartyId()))))
                .attachments(List.of(new DigitalMailAttachment()
                    .filename(invoice.getFilename())
                    .contentType(APPLICATION_PDF)
                    .content(encodedInvoiceContent)));

            var response = client.sendDigitalMail(request);

            // We know that we have a single message with a single delivery - extract the status
            var status = response.getMessages().get(0).getDeliveries().get(0).getStatus();

            return status == MessageStatus.SENT ? SENT : NOT_SENT;
        } catch (Exception e) {
e.printStackTrace(System.err); // TODO: proper handling/logging...
            return NOT_SENT;
        }
    }
}
