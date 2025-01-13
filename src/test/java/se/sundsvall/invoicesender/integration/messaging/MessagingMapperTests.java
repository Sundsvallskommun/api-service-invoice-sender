package se.sundsvall.invoicesender.integration.messaging;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.assertj.core.api.Assertions.assertThat;
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
	private MessagingIntegrationProperties properties;

	@Mock
	private MessagingIntegrationProperties.Invoice mockInvoiceProperties;

	@Mock
	private MessagingIntegrationProperties.StatusReport mockStatusReportProperties;

	@InjectMocks
	private MessagingMapper mapper;

	private String testFilePath;

	@BeforeEach
	void setUp() throws IOException {
		testFilePath = new ClassPathResource("files").getFile().getAbsolutePath();
	}

	@Test
	void toDigitalInvoiceRequest() throws IOException {
		var invoice = createItemEntity(item -> item.setFilename("test.file"));

		when(properties.invoice()).thenReturn(mockInvoiceProperties);
		when(mockInvoiceProperties.subject()).thenReturn("someSubject");
		when(mockInvoiceProperties.referencePrefix()).thenReturn("someReferencePrefix");
		when(mockFileSystem.getPath(testFilePath)).thenReturn(Paths.get(testFilePath));

		var result = mapper.toDigitalInvoiceRequest(invoice, testFilePath);

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
	}

	@Test
	void toEmailRequest() {
		var htmlMessage = "someHtmlMessage";

		when(properties.statusReport()).thenReturn(mockStatusReportProperties);
		when(mockStatusReportProperties.senderName()).thenReturn("someSenderName");
		when(mockStatusReportProperties.senderEmailAddress()).thenReturn("someSenderEmailAddress");
		when(mockStatusReportProperties.subjectPrefix()).thenReturn("someSubjectPrefix");

		var result = mapper.toEmailRequest(htmlMessage);

		assertThat(result).isNotNull();
		assertThat(result.getSender()).satisfies(sender -> {
			assertThat(sender.getName()).isEqualTo("someSenderName");
			assertThat(sender.getAddress()).isEqualTo("someSenderEmailAddress");
		});
		assertThat(result.getSubject()).isEqualTo("someSubjectPrefix " + ISO_DATE.format(LocalDate.now()));
		assertThat(result.getHtmlMessage()).isEqualTo(htmlMessage);
	}
}
