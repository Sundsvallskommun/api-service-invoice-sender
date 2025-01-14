package se.sundsvall.invoicesender.integration.messaging;

import static generated.se.sundsvall.messaging.Details.AccountTypeEnum.BANKGIRO;
import static generated.se.sundsvall.messaging.Details.PaymentReferenceTypeEnum.SE_OCR;
import static generated.se.sundsvall.messaging.DigitalInvoiceFile.ContentTypeEnum.APPLICATION_PDF;
import static generated.se.sundsvall.messaging.DigitalInvoiceRequest.TypeEnum.INVOICE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_DATE;

import generated.se.sundsvall.messaging.Details;
import generated.se.sundsvall.messaging.DigitalInvoiceFile;
import generated.se.sundsvall.messaging.DigitalInvoiceParty;
import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailSender;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;

@Component
public class MessagingMapper {

	private final MessagingIntegrationProperties properties;
	private final FileSystem fileSystem;

	MessagingMapper(final MessagingIntegrationProperties properties, final FileSystem fileSystem) {
		this.properties = properties;
		this.fileSystem = fileSystem;
	}

	public DigitalInvoiceRequest toDigitalInvoiceRequest(final ItemEntity invoice, final String path) throws IOException {
		final var invoiceContent = Files.readAllBytes(fileSystem.getPath(path).resolve(invoice.getFilename()));
		final var encodedInvoiceContent = new String(Base64.getEncoder().encode(invoiceContent), UTF_8);

		return new DigitalInvoiceRequest()
			.type(INVOICE)
			.subject(properties.invoice().subject())
			.party(new DigitalInvoiceParty().partyId(UUID.fromString(invoice.getRecipientPartyId())))
			.reference(properties.invoice().referencePrefix() + invoice.getMetadata().getInvoiceNumber())
			.payable(invoice.getMetadata().isPayable())
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
	}

	public EmailRequest toEmailRequest(final String htmlMessage) {
		return new EmailRequest()
			.sender(new EmailSender()
				.name(properties.statusReport().senderName())
				.address(properties.statusReport().senderEmailAddress()))
			.subject(properties.statusReport().subjectPrefix().trim().concat(" ") + ISO_DATE.format(LocalDate.now()))
			.htmlMessage(htmlMessage);
	}

}
