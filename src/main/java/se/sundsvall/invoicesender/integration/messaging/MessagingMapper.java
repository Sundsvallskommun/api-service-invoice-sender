package se.sundsvall.invoicesender.integration.messaging;

import static generated.se.sundsvall.messaging.Details.AccountTypeEnum.BANKGIRO;
import static generated.se.sundsvall.messaging.Details.PaymentReferenceTypeEnum.SE_OCR;
import static generated.se.sundsvall.messaging.DigitalInvoiceFile.ContentTypeEnum.APPLICATION_PDF;
import static generated.se.sundsvall.messaging.DigitalInvoiceRequest.TypeEnum.INVOICE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.messaging.Details;
import generated.se.sundsvall.messaging.DigitalInvoiceFile;
import generated.se.sundsvall.messaging.DigitalInvoiceParty;
import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.SlackRequest;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;

@Component
public class MessagingMapper {

	private final MessagingIntegrationProperties properties;

	MessagingMapper(final MessagingIntegrationProperties properties) {
		this.properties = properties;
	}

	public DigitalInvoiceRequest toDigitalInvoiceRequest(final ItemEntity invoice) {
		var encodedInvoiceContent = new String(Base64.getEncoder().encode(invoice.getData()), UTF_8);

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

	public EmailRequest toStatusEmailRequest(final String htmlMessage, final LocalDate date) {
		return toEmailRequest(htmlMessage, properties.statusReport().subjectPrefix(), date, properties.statusReport().senderName(), properties.statusReport().senderEmailAddress());
	}

	public EmailRequest toErrorEmailRequest(final String htmlMessage, final LocalDate date) {
		return toEmailRequest(htmlMessage, properties.errorReport().subjectPrefix(), date, properties.errorReport().senderName(), properties.errorReport().senderEmailAddress());
	}

	private EmailRequest toEmailRequest(final String htmlMessage, final String subjectPrefix, final LocalDate date, final String senderName, final String senderEmailAddress) {
		return new EmailRequest()
			.sender(new EmailSender()
				.name(senderName)
				.address(senderEmailAddress))
			.subject(ofNullable(subjectPrefix).orElse("").concat(" ") + ISO_DATE.format(date).trim())
			.htmlMessage(htmlMessage);
	}

	public SlackRequest toSlackRequest(String message) {
		return new SlackRequest()
			.message(message)
			.token(properties.token())
			.channel(properties.channel());
	}
}
