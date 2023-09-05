package se.sundsvall.invoicesender.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import se.sundsvall.invoicesender.model.Item;
import se.sundsvall.invoicesender.model.Status;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;

@ExtendWith(MockitoExtension.class)
class MessagingIntegrationTests {

    @Mock
    private MessagingIntegrationProperties mockIntegrationProperties;
    @Mock
    private MessagingIntegrationProperties.Invoice mockInvoiceProperties;
    @Mock
    private MessagingIntegrationProperties.StatusReport mockStatusReportProperties;
    @Mock
    private MessagingClient mockClient;
    @Mock
    private ITemplateEngine mockTemplateEngine;

    private MessagingIntegration messagingIntegration;

    @BeforeEach
    void setUp() {
        when(mockInvoiceProperties.subject()).thenReturn("someSubject");
        when(mockInvoiceProperties.referencePrefix()).thenReturn("somePrefix");

        when(mockStatusReportProperties.recipientEmailAddress()).thenReturn("someRecipientEmailAddress");
        when(mockStatusReportProperties.senderName()).thenReturn("someSenderName");
        when(mockStatusReportProperties.senderEmailAddress()).thenReturn("someSenderEmailAddress");
        when(mockStatusReportProperties.subjectPrefix()).thenReturn("somePrefix");

        when(mockIntegrationProperties.invoice()).thenReturn(mockInvoiceProperties);
        when(mockIntegrationProperties.statusReport()).thenReturn(mockStatusReportProperties);

        messagingIntegration = new MessagingIntegration(mockIntegrationProperties, mockClient, mockTemplateEngine);
    }

    @Test
    void testSendInvoice() {
        when(mockClient.sendDigitalInvoice(any(DigitalInvoiceRequest.class)))
            .thenReturn(new MessageBatchResult()
                .messages(List.of(new MessageResult()
                    .deliveries(List.of(new DeliveryResult()
                        .status(MessageStatus.SENT))))));

        var invoice = createMockInvoiceItem();

        var result = messagingIntegration.sendInvoice("somePath", invoice);
        assertThat(result).isEqualTo(Status.SENT);

        verify(mockClient, times(1)).sendDigitalInvoice(any(DigitalInvoiceRequest.class));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void testSendInvoiceWhenExceptionIsThrown() {
        when(mockClient.sendDigitalInvoice(any(DigitalInvoiceRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        var result = messagingIntegration.sendInvoice("somePath", createMockInvoiceItem());
        assertThat(result).isEqualTo(Status.NOT_SENT);

        verify(mockClient, times(1)).sendDigitalInvoice(any(DigitalInvoiceRequest.class));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void testSendStatusReport() {
        when(mockTemplateEngine.process(any(String.class),any(Context.class)))
            .thenReturn("someHtmlMessage");

        messagingIntegration.sendStatusReport(List.of());

        verify(mockClient, times(1)).sendEmail(any(EmailRequest.class));
        verifyNoMoreInteractions(mockClient);
        verify(mockTemplateEngine, times(1)).process(any(String.class), any(Context.class));
        verifyNoMoreInteractions(mockTemplateEngine);
    }

    @Test
    void testSendStatusReportWhenExceptionIsThrown() {
        when(mockClient.sendEmail(any(EmailRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        when(mockTemplateEngine.process(any(String.class),any(Context.class)))
            .thenReturn("someHtmlMessage");

        messagingIntegration.sendStatusReport(List.of());

        verify(mockClient, times(1)).sendEmail(any(EmailRequest.class));
        verifyNoMoreInteractions(mockClient);
        verify(mockTemplateEngine, times(1)).process(any(String.class), any(Context.class));
        verifyNoMoreInteractions(mockTemplateEngine);
    }

    private static Item createMockInvoiceItem() {
        return new Item()
            .setFilename("/dev/null")
            .setRecipientPartyId(UUID.randomUUID().toString())
            .setMetadata(new Item.Metadata()
                .setTotalAmount("12.34")
                .setDueDate("1986-02-26")
            );
    }
}
