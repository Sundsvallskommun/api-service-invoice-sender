package se.sundsvall.invoicesender.integration.messaging;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import se.sundsvall.invoicesender.model.Item;
import se.sundsvall.invoicesender.model.Status;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
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
	private String testFilePath;

	@BeforeEach
	void setUp() throws IOException {
		testFilePath = new ClassPathResource("files").getFile().getAbsolutePath();

		when(mockInvoiceProperties.subject()).thenReturn("someSubject");
		when(mockInvoiceProperties.referencePrefix()).thenReturn("somePrefix");

		when(mockStatusReportProperties.recipientEmailAddresses()).thenReturn(List.of("someRecipientEmailAddress"));
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
			.thenReturn(new MessageResult()
				.deliveries(List.of(new DeliveryResult()
					.status(MessageStatus.SENT))));

		final var invoice = createMockInvoiceItem();

		final var result = messagingIntegration.sendInvoice(testFilePath, invoice);
		assertThat(result).isEqualTo(Status.SENT);

		verify(mockClient, times(1)).sendDigitalInvoice(any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@Test
	void testSendInvoiceWhenExceptionIsThrown() {
		when(mockClient.sendDigitalInvoice(any(DigitalInvoiceRequest.class)))
			.thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

		final var result = messagingIntegration.sendInvoice(testFilePath, createMockInvoiceItem());
		assertThat(result).isEqualTo(Status.NOT_SENT);

		verify(mockClient, times(1)).sendDigitalInvoice(any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@Test
	void testSendStatusReport() {
		when(mockTemplateEngine.process(any(String.class), any(Context.class)))
			.thenReturn("someHtmlMessage");

		messagingIntegration.sendStatusReport(emptyList());

		verify(mockClient, times(1)).sendEmail(any(EmailRequest.class));
		verifyNoMoreInteractions(mockClient);
		verify(mockTemplateEngine, times(1)).process(any(String.class), any(Context.class));
		verifyNoMoreInteractions(mockTemplateEngine);
	}

	@Test
	void testSendStatusReportWhenExceptionIsThrown() {
		when(mockClient.sendEmail(any(EmailRequest.class)))
			.thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

		when(mockTemplateEngine.process(any(String.class), any(Context.class)))
			.thenReturn("someHtmlMessage");

		messagingIntegration.sendStatusReport(emptyList());

		verify(mockClient, times(1)).sendEmail(any(EmailRequest.class));
		verifyNoMoreInteractions(mockClient);
		verify(mockTemplateEngine, times(1)).process(any(String.class), any(Context.class));
		verifyNoMoreInteractions(mockTemplateEngine);
	}

	private static Item createMockInvoiceItem() {
		return new Item()
			.setFilename("test.file")
			.setRecipientPartyId(UUID.randomUUID().toString())
			.setMetadata(new Item.Metadata()
				.setTotalAmount("12.34")
				.setDueDate("1986-02-26"));
	}
}
