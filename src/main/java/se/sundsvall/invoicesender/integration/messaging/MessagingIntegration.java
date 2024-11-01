package se.sundsvall.invoicesender.integration.messaging;

import static generated.se.sundsvall.messaging.Details.AccountTypeEnum.BANKGIRO;
import static generated.se.sundsvall.messaging.Details.PaymentReferenceTypeEnum.SE_OCR;
import static generated.se.sundsvall.messaging.DigitalInvoiceFile.ContentTypeEnum.APPLICATION_PDF;
import static generated.se.sundsvall.messaging.DigitalInvoiceRequest.TypeEnum.INVOICE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static se.sundsvall.invoicesender.model.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import se.sundsvall.invoicesender.integration.db.dto.BatchDto;
import se.sundsvall.invoicesender.model.Item;
import se.sundsvall.invoicesender.model.ItemStatus;

import generated.se.sundsvall.messaging.Details;
import generated.se.sundsvall.messaging.DigitalInvoiceFile;
import generated.se.sundsvall.messaging.DigitalInvoiceParty;
import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.MessageStatus;

@Component
public class MessagingIntegration {

	static final String INTEGRATION_NAME = "Messaging";

	private static final Logger LOG = LoggerFactory.getLogger(MessagingIntegration.class);

	private final MessagingClient client;

	private final ITemplateEngine templateEngine;

	private final String invoiceSubject;

	private final String invoiceReferencePrefix;

	private final String statusReportSenderName;

	private final String statusReportSenderEmailAddress;

	private final List<String> statusReportRecipientEmailAddresses;

	private String statusReportSubjectPrefix;

	MessagingIntegration(final MessagingIntegrationProperties properties,
		final MessagingClient client, final ITemplateEngine templateEngine) {
		this.client = client;
		this.templateEngine = templateEngine;

		invoiceSubject = properties.invoice().subject();
		invoiceReferencePrefix = properties.invoice().referencePrefix();

		statusReportSenderName = properties.statusReport().senderName();
		statusReportSenderEmailAddress = properties.statusReport().senderEmailAddress();
		statusReportRecipientEmailAddresses = properties.statusReport().recipientEmailAddresses();
		statusReportSubjectPrefix = properties.statusReport().subjectPrefix();
		if (!statusReportSubjectPrefix.endsWith(" ")) {
			statusReportSubjectPrefix += " ";
		}
	}

	public ItemStatus sendInvoice(final String path, final Item invoice, final String municipalityId) {
		try {
			final var invoiceContent = Files.readAllBytes(Paths.get(path).resolve(invoice.getFilename()));
			final var encodedInvoiceContent = new String(Base64.getEncoder().encode(invoiceContent), UTF_8);

			final var request = new DigitalInvoiceRequest()
				.type(INVOICE)
				.subject(invoiceSubject)
				.party(new DigitalInvoiceParty().partyId(UUID.fromString(invoice.getRecipientPartyId())))
				.reference(invoiceReferencePrefix + invoice.getMetadata().getInvoiceNumber())
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

			final var response = client.sendDigitalInvoice(municipalityId, request);

			// We know that we have a single message with a single delivery - extract the status
			final var status = response.getDeliveries().getFirst().getStatus();

			return status == MessageStatus.SENT ? SENT : NOT_SENT;
		} catch (final Exception e) {
			LOG.warn("Unable to send invoice", e);

			return NOT_SENT;
		}
	}

	public void sendStatusReport(final List<BatchDto> batches, final String municipalityId) {
		LOG.info("Sending status report");
		final var request = new EmailRequest()
			.sender(new EmailSender()
				.name(statusReportSenderName)
				.address(statusReportSenderEmailAddress))
			.subject(statusReportSubjectPrefix + ISO_DATE.format(LocalDate.now()))
			.htmlMessage(generateStatusReportMessage(batches));

		for (final var recipientEmailAddress : statusReportRecipientEmailAddresses) {
			try {
				request.setEmailAddress(recipientEmailAddress);

				client.sendEmail(municipalityId, request);
			} catch (final Exception e) {
				LOG.warn("Unable to send status report to " + recipientEmailAddress, e);
			}
		}
	}

	String generateStatusReportMessage(final List<BatchDto> batches) {
		final var context = new Context();
		context.setVariable("batches", batches);
		final var htmlMessage = templateEngine.process("status-report", context);
		return Base64.getEncoder().encodeToString(htmlMessage.getBytes(UTF_8));
	}

}
