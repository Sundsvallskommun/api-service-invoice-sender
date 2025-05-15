package se.sundsvall.invoicesender.integration.messaging;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.invoicesender.TestDataFactory.createItemEntity;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.OTHER;
import static se.sundsvall.invoicesender.integration.messaging.MessagingIntegration.ERROR_TEMPLATE_NAME;
import static se.sundsvall.invoicesender.integration.messaging.MessagingIntegration.STATUS_TEMPLATE_NAME;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;
import generated.se.sundsvall.messaging.SlackRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;

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
	private MessagingIntegrationProperties.ErrorReport mockErrorReportProperties;

	@Mock
	private MessagingClient mockClient;

	@Mock
	private EmailRequest mockEmailRequest;

	@Mock
	private ITemplateEngine mockTemplateEngine;

	@Mock
	private MessagingMapper messagingMapper;

	@Captor
	private ArgumentCaptor<Context> contextCaptor;

	@InjectMocks
	private MessagingIntegration messagingIntegration;

	private String testFilePath;

	@BeforeEach
	void setUp() throws IOException {
		testFilePath = new ClassPathResource("files").getFile().getAbsolutePath();
	}

	@Test
	void testSendInvoiceSuccessful() throws IOException {
		final var invoice = createItemEntity(item -> item.setFilename("test.file"));

		when(messagingMapper.toDigitalInvoiceRequest(invoice, testFilePath)).thenReturn(new DigitalInvoiceRequest());
		when(mockClient.sendDigitalInvoice(any(String.class), any(DigitalInvoiceRequest.class)))
			.thenReturn(new MessageResult()
				.deliveries(List.of(new DeliveryResult()
					.status(MessageStatus.SENT))));

		final var result = messagingIntegration.sendInvoice(testFilePath, invoice, MUNICIPALITY_ID);
		assertThat(result).isEqualTo(SENT);

		verify(mockClient).sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@ParameterizedTest
	@EnumSource(value = MessageStatus.class, mode = Mode.EXCLUDE, names = "SENT")
	void testSendInvoiceFailed(MessageStatus resultStatus) throws IOException {
		final var invoice = createItemEntity(item -> item.setFilename("test.file"));

		when(messagingMapper.toDigitalInvoiceRequest(invoice, testFilePath)).thenReturn(new DigitalInvoiceRequest());
		when(mockClient.sendDigitalInvoice(any(String.class), any(DigitalInvoiceRequest.class)))
			.thenReturn(new MessageResult()
				.deliveries(List.of(new DeliveryResult()
					.status(resultStatus))));

		final var result = messagingIntegration.sendInvoice(testFilePath, invoice, MUNICIPALITY_ID);
		assertThat(result).isEqualTo(NOT_SENT);

		verify(mockClient).sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@Test
	void testSendInvoiceWhenExceptionIsThrown() throws IOException {
		final var invoice = createItemEntity(item -> item.setFilename("test.file"));

		when(messagingMapper.toDigitalInvoiceRequest(invoice, testFilePath)).thenReturn(new DigitalInvoiceRequest());
		when(mockClient.sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class)))
			.thenThrow(new ResponseStatusException(INTERNAL_SERVER_ERROR));

		final var result = messagingIntegration.sendInvoice(testFilePath, invoice, MUNICIPALITY_ID);

		assertThat(result).isEqualTo(NOT_SENT);

		verify(mockClient).sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@Test
	void testSendInvoiceWhenCertificateProblemIsThrown() throws IOException {
		final var invoice = createItemEntity(item -> item.setFilename("test.file"));
		final var certificateException = Problem.valueOf(Status.BAD_GATEWAY, "prefix [invalid_token_response] suffix");

		when(messagingMapper.toDigitalInvoiceRequest(invoice, testFilePath)).thenReturn(new DigitalInvoiceRequest());
		when(mockClient.sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class)))
			.thenThrow(certificateException);

		final var e = assertThrows(ThrowableProblem.class, () -> messagingIntegration.sendInvoice(testFilePath, invoice, MUNICIPALITY_ID));

		assertThat(e).isSameAs(certificateException);
		verify(mockClient).sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@ParameterizedTest
	@ValueSource(strings = "non matching message")
	@NullSource
	void testSendInvoiceWhenOtherProblemIsThrown(String message) throws IOException {
		final var invoice = createItemEntity(item -> item.setFilename("test.file"));

		when(messagingMapper.toDigitalInvoiceRequest(invoice, testFilePath)).thenReturn(new DigitalInvoiceRequest());
		when(mockClient.sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class)))
			.thenThrow(Problem.valueOf(Status.BAD_GATEWAY, message));

		final var result = messagingIntegration.sendInvoice(testFilePath, invoice, MUNICIPALITY_ID);

		assertThat(result).isEqualTo(NOT_SENT);

		verify(mockClient).sendDigitalInvoice(eq(MUNICIPALITY_ID), any(DigitalInvoiceRequest.class));
		verifyNoMoreInteractions(mockClient);
	}

	@Test
	void testSendErrorReport() {
		final var batchName = "batchName";
		final var date = LocalDate.now();
		final var message = "message";
		final var recipientEmailAddress = "Recipeint@test.se";
		final var recipientEmailAddresses = List.of(recipientEmailAddress);

		when(mockIntegrationProperties.errorReport()).thenReturn(mockErrorReportProperties);
		when(mockErrorReportProperties.recipientEmailAddresses()).thenReturn(recipientEmailAddresses);
		when(mockTemplateEngine.process(eq(ERROR_TEMPLATE_NAME), any(Context.class))).thenReturn(HTML_MESSAGE);
		when(messagingMapper.toErrorEmailRequest(ENCODED_HTML_MESSAGE, date)).thenReturn(mockEmailRequest);

		messagingIntegration.sendErrorReport(date, MUNICIPALITY_ID, batchName, message);

		verify(mockEmailRequest).setEmailAddress(recipientEmailAddress);
		verify(messagingMapper).toErrorEmailRequest(ENCODED_HTML_MESSAGE, date);
		verify(mockClient).sendEmail(MUNICIPALITY_ID, mockEmailRequest);
		verify(mockTemplateEngine).process(eq(ERROR_TEMPLATE_NAME), any(Context.class));
		verifyNoMoreInteractions(mockClient, mockTemplateEngine);
	}

	@ParameterizedTest
	@MethodSource("sendReportArgumentProvider")
	void testSendErrorReportWhenExcpetionIsThrown(List<String> recipientEmailAddresses, int expectedNumberOfCalls) {
		final var batchName = "batchName";
		final var date = LocalDate.now();
		final var message = "message";

		when(mockIntegrationProperties.errorReport()).thenReturn(mockErrorReportProperties);
		when(mockErrorReportProperties.recipientEmailAddresses()).thenReturn(recipientEmailAddresses);
		when(mockTemplateEngine.process(eq(ERROR_TEMPLATE_NAME), any(Context.class))).thenReturn(HTML_MESSAGE);
		when(messagingMapper.toErrorEmailRequest(ENCODED_HTML_MESSAGE, date)).thenReturn(mockEmailRequest);
		when(mockClient.sendEmail(MUNICIPALITY_ID, mockEmailRequest)).thenThrow(new ResponseStatusException(INTERNAL_SERVER_ERROR));

		messagingIntegration.sendErrorReport(date, MUNICIPALITY_ID, batchName, message);

		recipientEmailAddresses.stream().forEach(recipientEmailAddress -> verify(mockEmailRequest).setEmailAddress(recipientEmailAddress));
		verify(messagingMapper).toErrorEmailRequest(ENCODED_HTML_MESSAGE, date);
		verify(mockClient, times(expectedNumberOfCalls)).sendEmail(MUNICIPALITY_ID, mockEmailRequest);
		verify(mockTemplateEngine).process(eq(ERROR_TEMPLATE_NAME), any(Context.class));
		verifyNoMoreInteractions(mockTemplateEngine, mockClient);
	}

	@Test
	void testSendStatusReport() {
		final var date = LocalDate.now();
		final List<BatchEntity> batches = emptyList();
		final var recipientEmailAddress = "Recipeint@test.se";
		final List<String> recipientEmailAddresses = List.of(recipientEmailAddress);

		when(mockIntegrationProperties.statusReport()).thenReturn(mockStatusReportProperties);
		when(mockStatusReportProperties.recipientEmailAddresses()).thenReturn(recipientEmailAddresses);

		when(mockTemplateEngine.process(eq(STATUS_TEMPLATE_NAME), any(Context.class))).thenReturn(HTML_MESSAGE);
		when(messagingMapper.toStatusEmailRequest(ENCODED_HTML_MESSAGE, date)).thenReturn(mockEmailRequest);

		messagingIntegration.sendStatusReport(batches, date, MUNICIPALITY_ID);

		verify(mockEmailRequest).setEmailAddress(recipientEmailAddress);
		verify(messagingMapper).toStatusEmailRequest(ENCODED_HTML_MESSAGE, date);
		verify(mockClient).sendEmail(MUNICIPALITY_ID, mockEmailRequest);
		verify(mockTemplateEngine).process(eq(STATUS_TEMPLATE_NAME), any(Context.class));
		verifyNoMoreInteractions(mockClient, mockTemplateEngine);
	}

	@ParameterizedTest
	@MethodSource("sendReportArgumentProvider")
	void testSendStatusReportWhenExceptionIsThrown(List<String> recipientEmailAddresses, int expectedNumberOfCalls) {
		final var date = LocalDate.now();
		final List<BatchEntity> batches = emptyList();

		when(mockIntegrationProperties.statusReport()).thenReturn(mockStatusReportProperties);
		when(mockStatusReportProperties.recipientEmailAddresses()).thenReturn(recipientEmailAddresses);

		when(mockTemplateEngine.process(eq(STATUS_TEMPLATE_NAME), any(Context.class))).thenReturn(HTML_MESSAGE);
		when(messagingMapper.toStatusEmailRequest(ENCODED_HTML_MESSAGE, date)).thenReturn(mockEmailRequest);
		when(mockClient.sendEmail(MUNICIPALITY_ID, mockEmailRequest)).thenThrow(new ResponseStatusException(INTERNAL_SERVER_ERROR));

		messagingIntegration.sendStatusReport(batches, date, MUNICIPALITY_ID);

		recipientEmailAddresses.stream().forEach(recipientEmailAddress -> verify(mockEmailRequest).setEmailAddress(recipientEmailAddress));
		verify(messagingMapper).toStatusEmailRequest(ENCODED_HTML_MESSAGE, date);
		verify(mockClient, times(expectedNumberOfCalls)).sendEmail(MUNICIPALITY_ID, mockEmailRequest);
		verify(mockTemplateEngine).process(eq(STATUS_TEMPLATE_NAME), any(Context.class));
		verifyNoMoreInteractions(mockTemplateEngine, mockClient);
	}

	private static Stream<Arguments> sendReportArgumentProvider() {
		return Stream.of(
			Arguments.of(List.of("Recipient@test.se"), 1),
			Arguments.of(List.of("Recipient@test.se", "Recipient2@test.se"), 2));
	}

	@Test
	void testSendSlackMessage() {
		final var batch = new BatchEntity();
		final var date = LocalDate.now();
		final var slackRequest = new SlackRequest()
			.channel("Test-Channel")
			.token("Test-Token")
			.message("Clark Kent is a fraud");

		when(messagingMapper.toSlackRequest(anyString())).thenReturn(slackRequest);

		messagingIntegration.sendSlackMessage(batch, date, MUNICIPALITY_ID);

		verify(mockClient).sendSlackMessage(MUNICIPALITY_ID, slackRequest);
		verify(messagingMapper).toSlackRequest(anyString());
		verifyNoMoreInteractions(mockClient, messagingMapper);
	}

	@Test
	void testSendSlackMessageWhenExceptionIsThrown() {
		final var batch = new BatchEntity();
		final var date = LocalDate.now();
		final var slackRequest = new SlackRequest();

		when(messagingMapper.toSlackRequest(anyString())).thenReturn(slackRequest);
		when(mockClient.sendSlackMessage(MUNICIPALITY_ID, slackRequest))
			.thenThrow(new ResponseStatusException(INTERNAL_SERVER_ERROR));

		assertThatNoException().isThrownBy(() -> messagingIntegration.sendSlackMessage(batch, date, MUNICIPALITY_ID));

		verify(messagingMapper).toSlackRequest(anyString());
		verify(mockClient).sendSlackMessage(MUNICIPALITY_ID, slackRequest);
		verifyNoMoreInteractions(mockClient, messagingMapper);
	}

	@Test
	void testGenerateErrorReportMessage() {
		RequestId.init();

		final var batchName = "batchName";
		when(mockTemplateEngine.process(eq(ERROR_TEMPLATE_NAME), any(Context.class))).thenReturn(HTML_MESSAGE);

		final var result = messagingIntegration.generateErrorReportMessage(MUNICIPALITY_ID, batchName, HTML_MESSAGE);

		verify(mockTemplateEngine).process(eq(ERROR_TEMPLATE_NAME), contextCaptor.capture());

		assertThat(result).isEqualTo(ENCODED_HTML_MESSAGE);
		assertThat(contextCaptor.getValue()).satisfies(context -> {
			assertThat(context.getVariableNames()).hasSize(4);
			assertThat(context.getVariable("requestId")).isEqualTo(RequestId.get());
			assertThat(context.getVariable("batchName")).isEqualTo(batchName);
			assertThat(context.getVariable("municipalityId")).isEqualTo(MUNICIPALITY_ID);
			assertThat(context.getVariable("message")).isEqualTo(HTML_MESSAGE);
		});
	}

	@Test
	void testGenerateSlackMessage() {
		final var batch = new BatchEntity()
			.withBasename("testBasename")
			.withItems(List.of(
				new ItemEntity().withType(OTHER).withStatus(IGNORED), // Check that ArchiveIndex.xml is not counted as an invoice.
				new ItemEntity().withType(INVOICE).withStatus(SENT),
				new ItemEntity().withType(INVOICE).withStatus(NOT_SENT)))
			.withSentItems(1);
		final var date = LocalDate.of(2025, 2, 28);

		final var message = messagingIntegration.generateSlackMessage(batch, date);

		final var expected = """
			Batch: testBasename
			Date: 2025-02-28
			Invoices sent: 1
			Invoices not sent: 1
			""";
		assertThat(message).isEqualTo(expected);
	}
}
