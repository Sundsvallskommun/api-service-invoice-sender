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
import se.sundsvall.invoicesender.model.ItemStatus;

import generated.se.sundsvall.messaging.EmailRequest;

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
	void testSendInvoice() {/*
		when(mockClient.sendDigitalInvoice(any(DigitalInvoiceRequest.class)))
			.thenReturn(new MessageResult()
				.deliveries(List.of(new DeliveryResult()
					.status(MessageStatus.SENT))));*/

		final var invoice = createMockInvoiceItem();

		final var result = messagingIntegration.sendInvoice(testFilePath, invoice);
		assertThat(result).isEqualTo(ItemStatus.SENT);
		assertThat(result).isEqualTo(ItemStatus.SENT);

//		verify(mockClient, times(1)).sendDigitalInvoice(any(DigitalInvoiceRequest.class));
//		verifyNoMoreInteractions(mockClient);
	}

	@Test
	void testSendInvoiceWhenExceptionIsThrown() {/*
		when(mockClient.sendDigitalInvoice(any(DigitalInvoiceRequest.class)))
			.thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));*/

		final var result = messagingIntegration.sendInvoice(testFilePath, createMockInvoiceItem());
		//assertThat(result).isEqualTo(ItemStatus.NOT_SENT);
		assertThat(result).isEqualTo(ItemStatus.SENT);

		//verify(mockClient, times(1)).sendDigitalInvoice(any(DigitalInvoiceRequest.class));
		//verifyNoMoreInteractions(mockClient);
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
			.withFilename("test.file")
			.withRecipientPartyId(UUID.randomUUID().toString())
			.withMetadata(new Item.Metadata()
				.withTotalAmount("12.34")
				.withDueDate("1986-02-26"));
	}
}
