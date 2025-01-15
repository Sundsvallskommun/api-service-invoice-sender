package se.sundsvall.invoicesender.integration.messaging;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.invoicesender.TestDataFactory.createItemEntity;
import static se.sundsvall.invoicesender.integration.messaging.MessagingIntegration.TEMPLATE_NAME;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemStatus;

@ExtendWith(MockitoExtension.class)
class MessagingIntegrationTests {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String HTML_MESSAGE = "someHtmlMessage";
	private static final String ENCODED_HTML_MESSAGE = "c29tZUh0bWxNZXNzYWdl";

	@Mock
	private MessagingIntegrationProperties mockIntegrationProperties;

	@Mock
	private MessagingIntegrationProperties.StatusReport mockStatusReportProperties;

	@Mock
	private MessagingClient mockClient;

	@Mock
	private ITemplateEngine mockTemplateEngine;

	@Mock
	private MessagingMapper messagingMapper;

	@InjectMocks
	private MessagingIntegration messagingIntegration;

	private String testFilePath;

	@BeforeEach
	void setUp() throws IOException {
		testFilePath = new ClassPathResource("files").getFile().getAbsolutePath();
	}

	@Test
	void testSendInvoice() throws IOException {
		var invoice = createItemEntity(item -> item.setFilename("test.file"));

		when(messagingMapper.toDigitalInvoiceRequest(invoice, testFilePath)).thenReturn(new DigitalInvoiceRequest());
		when(mockClient.sendDigitalInvoice(any(String.class), any(DigitalInvoiceRequest.class)))
			.thenReturn(new MessageResult()
				.deliveries(List.of(new DeliveryResult()
					.status(MessageStatus.SENT))));

		var result = messagingIntegration.sendInvoice(testFilePath, invoice, MUNICIPALITY_ID);
		assertThat(result).isEqualTo(ItemStatus.SENT);

		verify(mockClient).sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@Test
	void testSendInvoiceWhenExceptionIsThrown() throws IOException {
		var invoice = createItemEntity(item -> item.setFilename("test.file"));

		when(messagingMapper.toDigitalInvoiceRequest(invoice, testFilePath)).thenReturn(new DigitalInvoiceRequest());
		when(mockClient.sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class)))
			.thenThrow(new ResponseStatusException(INTERNAL_SERVER_ERROR));

		var result = messagingIntegration.sendInvoice(testFilePath, invoice, MUNICIPALITY_ID);

		assertThat(result).isEqualTo(ItemStatus.NOT_SENT);

		verify(mockClient).sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@Test
	void testSendStatusReport() {
		var date = LocalDate.now();
		List<BatchEntity> batches = emptyList();
		List<String> recipientEmailAddresses = List.of("Recipeint@test.se");
		var emailRequest = new EmailRequest();

		when(mockIntegrationProperties.statusReport()).thenReturn(mockStatusReportProperties);
		when(mockStatusReportProperties.recipientEmailAddresses()).thenReturn(recipientEmailAddresses);

		when(mockTemplateEngine.process(eq(TEMPLATE_NAME), any(Context.class))).thenReturn(HTML_MESSAGE);
		when(messagingMapper.toEmailRequest(ENCODED_HTML_MESSAGE, date)).thenReturn(emailRequest);

		messagingIntegration.sendStatusReport(batches, date, MUNICIPALITY_ID);

		verify(messagingMapper).toEmailRequest(ENCODED_HTML_MESSAGE, date);
		verify(mockClient).sendEmail(MUNICIPALITY_ID, emailRequest);
		verify(mockTemplateEngine).process(eq(TEMPLATE_NAME), any(Context.class));
		verifyNoMoreInteractions(mockClient, mockTemplateEngine);
	}

	@ParameterizedTest
	@MethodSource("sendStatusReportArgumentProvider")
	void testSendStatusReportWhenExceptionIsThrown(List<String> recipientEmailAddresses, int expectedNumberOfCalls) {
		var date = LocalDate.now();
		List<BatchEntity> batches = emptyList();
		var emailRequest = new EmailRequest();

		when(mockIntegrationProperties.statusReport()).thenReturn(mockStatusReportProperties);
		when(mockStatusReportProperties.recipientEmailAddresses()).thenReturn(recipientEmailAddresses);

		when(mockTemplateEngine.process(eq(TEMPLATE_NAME), any(Context.class))).thenReturn(HTML_MESSAGE);
		when(messagingMapper.toEmailRequest(ENCODED_HTML_MESSAGE, date)).thenReturn(emailRequest);
		when(mockClient.sendEmail(MUNICIPALITY_ID, emailRequest))
			.thenThrow(new ResponseStatusException(INTERNAL_SERVER_ERROR));

		messagingIntegration.sendStatusReport(batches, date, MUNICIPALITY_ID);

		verify(messagingMapper).toEmailRequest(ENCODED_HTML_MESSAGE, date);
		verify(mockClient, times(expectedNumberOfCalls)).sendEmail(MUNICIPALITY_ID, emailRequest);
		verify(mockTemplateEngine).process(eq(TEMPLATE_NAME), any(Context.class));
		verifyNoMoreInteractions(mockTemplateEngine, mockClient);
	}

	private static Stream<Arguments> sendStatusReportArgumentProvider() {
		return Stream.of(
			Arguments.of(List.of("Recipient@test.se"), 1),
			Arguments.of(List.of("Recipient@test.se", "Recipient2@test.se"), 2));
	}

}
