package se.sundsvall.invoicesender.integration.messaging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static se.sundsvall.invoicesender.model.Status.NOT_SENT;
import static se.sundsvall.invoicesender.model.Status.SENT;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.invoicesender.model.Item;
import se.sundsvall.invoicesender.model.Status;

@Component
public class MessagingIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingIntegration.class);

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
/*
            var request = new DigitalMailRequest()
                .subject(invoiceSubject)
                .party(new DigitalMailParty().partyIds(List.of(UUID.fromString(invoice.getRecipientPartyId()))))
                .attachments(List.of(new DigitalMailAttachment()
                    .filename(invoice.getFilename())
                    .contentType(APPLICATION_PDF)
                    .content(encodedInvoiceContent)));

            var response = client.sendDigitalInvoice(request);

            // We know that we have a single message with a single delivery - extract the status
            var status = response.getMessages().get(0).getDeliveries().get(0).getStatus();

            return status == MessageStatus.SENT ? SENT : NOT_SENT;*/
            return SENT;
        } catch (Exception e) {
            LOG.warn("Unable to send invoice", e);

            return NOT_SENT;
        }
    }
}
