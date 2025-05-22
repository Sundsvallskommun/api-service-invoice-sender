package se.sundsvall.invoicesender.integration.messaging;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.invoicesender.TestDataFactory.createItemEntity;

import generated.se.sundsvall.messaging.Details;
import generated.se.sundsvall.messaging.DigitalInvoiceFile;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class MessagingMapperTests {

	@Mock
	private FileSystem mockFileSystem;

	@Mock
	private MessagingIntegrationProperties mockProperties;

	@Mock
	private MessagingIntegrationProperties.Invoice mockInvoiceProperties;

	@Mock
	private MessagingIntegrationProperties.StatusReport mockStatusReportProperties;

	@Mock
	private MessagingIntegrationProperties.ErrorReport mockErrorReportProperties;

	@InjectMocks
	private MessagingMapper mapper;

	private String testFilePath;

	@BeforeEach
	void setUp() throws IOException {
		testFilePath = new ClassPathResource("files").getFile().getAbsolutePath();
	}

	@Test
	void toDigitalInvoiceRequest() throws IOException {
		final var invoice = createItemEntity(item -> item.setFilename("test.file"));

		when(mockProperties.invoice()).thenReturn(mockInvoiceProperties);
		when(mockInvoiceProperties.subject()).thenReturn("someSubject");
		when(mockInvoiceProperties.referencePrefix()).thenReturn("someReferencePrefix");
		when(mockFileSystem.getPath(testFilePath)).thenReturn(Paths.get(testFilePath));

		final var result = mapper.toDigitalInvoiceRequest(invoice, testFilePath);

		assertThat(result).isNotNull();
		assertThat(result.getSubject()).isEqualTo("someSubject");
		assertThat(result.getParty().getPartyId()).isEqualTo(UUID.fromString(invoice.getRecipientPartyId()));
		assertThat(result.getReference()).isEqualTo("someReferencePrefix" + invoice.getMetadata().getInvoiceNumber());
		assertThat(result.getPayable()).isEqualTo(invoice.getMetadata().isPayable());
		assertThat(result.getDetails()).satisfies(detail -> {
			assertThat(detail.getAmount()).isEqualTo(Float.valueOf(invoice.getMetadata().getTotalAmount()));
			assertThat(detail.getDueDate()).isEqualTo(invoice.getMetadata().getDueDate());
			assertThat(detail.getPaymentReferenceType()).isEqualTo(Details.PaymentReferenceTypeEnum.SE_OCR);
			assertThat(detail.getPaymentReference()).isEqualTo(invoice.getMetadata().getPaymentReference());
			assertThat(detail.getAccountType()).isEqualTo(Details.AccountTypeEnum.BANKGIRO);
			assertThat(detail.getAccountNumber()).isEqualTo(invoice.getMetadata().getAccountNumber());
		});
		assertThat(result.getFiles()).hasSize(1).first().satisfies(file -> {
			assertThat(file.getFilename()).isEqualTo(invoice.getFilename());
			assertThat(file.getContentType()).isEqualTo(DigitalInvoiceFile.ContentTypeEnum.APPLICATION_PDF);
			assertThat(file.getContent()).isBlank();
		});

		verify(mockInvoiceProperties).subject();
		verify(mockInvoiceProperties).referencePrefix();
		verify(mockFileSystem).getPath(testFilePath);
	}

	@Test
	void toStatusEmailRequest() {
		final var date = LocalDate.now();
		final var htmlMessage = "someStatusHtmlMessage";

		when(mockProperties.statusReport()).thenReturn(mockStatusReportProperties);
		when(mockStatusReportProperties.senderName()).thenReturn("someStatusSenderName");
		when(mockStatusReportProperties.senderEmailAddress()).thenReturn("someStatusSenderEmailAddress");
		when(mockStatusReportProperties.subjectPrefix()).thenReturn("someStatusSubjectPrefix");

		final var result = mapper.toStatusEmailRequest(htmlMessage, date);

		assertThat(result).isNotNull();
		assertThat(result.getSender()).satisfies(sender -> {
			assertThat(sender.getName()).isEqualTo("someStatusSenderName");
			assertThat(sender.getAddress()).isEqualTo("someStatusSenderEmailAddress");
		});
		assertThat(result.getSubject()).isEqualTo("someStatusSubjectPrefix " + ISO_DATE.format(LocalDate.now()));
		assertThat(result.getHtmlMessage()).isEqualTo(htmlMessage);
		assertThat(result.getHeaders()).isNullOrEmpty();

		verify(mockStatusReportProperties).senderName();
		verify(mockStatusReportProperties).senderEmailAddress();
		verify(mockStatusReportProperties).subjectPrefix();
	}

	@Test
	void toErrorEmailRequest() {
		final var date = LocalDate.now();
		final var htmlMessage = "someErrorHtmlMessage";

		when(mockProperties.errorReport()).thenReturn(mockErrorReportProperties);
		when(mockErrorReportProperties.senderName()).thenReturn("someErrorSenderName");
		when(mockErrorReportProperties.senderEmailAddress()).thenReturn("someErrorSenderEmailAddress");
		when(mockErrorReportProperties.subjectPrefix()).thenReturn("someErrorSubjectPrefix");

		final var result = mapper.toErrorEmailRequest(htmlMessage, date);

		assertThat(result).isNotNull();
		assertThat(result.getSender()).satisfies(sender -> {
			assertThat(sender.getName()).isEqualTo("someErrorSenderName");
			assertThat(sender.getAddress()).isEqualTo("someErrorSenderEmailAddress");
		});
		assertThat(result.getSubject()).isEqualTo("someErrorSubjectPrefix " + ISO_DATE.format(LocalDate.now()));
		assertThat(result.getHtmlMessage()).isEqualTo(htmlMessage);
		assertThat(result.getHeaders()).isNullOrEmpty();

		verify(mockErrorReportProperties).senderName();
		verify(mockErrorReportProperties).senderEmailAddress();
		verify(mockErrorReportProperties).subjectPrefix();
	}

	@Test
	void toSlackRequest() {
		final var message = "someMessage";

		when(mockProperties.token()).thenReturn("someToken");
		when(mockProperties.channel()).thenReturn("someChannel");

		final var result = mapper.toSlackRequest(message);

		assertThat(result).isNotNull();
		assertThat(result.getToken()).isEqualTo("someToken");
		assertThat(result.getChannel()).isEqualTo("someChannel");
		assertThat(result.getMessage()).isEqualTo("someMessage");
		assertThat(result).hasNoNullFieldsOrProperties();

		verify(mockProperties).token();
		verify(mockProperties).channel();
	}
}
