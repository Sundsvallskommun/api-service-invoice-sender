package se.sundsvall.invoicesender.integration.messaging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;

import generated.se.sundsvall.messaging.MessageStatus;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemStatus;

@Component
public class MessagingIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(MessagingIntegration.class);
	private static final String LOG_MESSAGE_STATUS_REPORT = "Status report sent to {}";

	static final String STATUS_TEMPLATE_NAME = "status-report";
	static final String ERROR_TEMPLATE_NAME = "error-report";

	private final MessagingIntegrationProperties properties;
	private final MessagingClient client;
	private final MessagingMapper messagingMapper;

	private final ITemplateEngine templateEngine;

	MessagingIntegration(final MessagingIntegrationProperties properties,
		final MessagingClient client,
		final MessagingMapper messagingMapper,
		final ITemplateEngine templateEngine) {
		this.client = client;
		this.messagingMapper = messagingMapper;
		this.templateEngine = templateEngine;
		this.properties = properties;
	}

	public ItemStatus sendInvoice(final String path, final ItemEntity invoice, final String municipalityId) {
		try {
			final var request = messagingMapper.toDigitalInvoiceRequest(invoice, path);

			final var response = client.sendDigitalInvoice(municipalityId, request);

			// We know that we have a single message with a single delivery - extract the status
			final var status = response.getDeliveries().getFirst().getStatus();

			return status == MessageStatus.SENT ? SENT : NOT_SENT;
		} catch (final ThrowableProblem e) {
			if (nonNull(e.getDetail()) && e.getDetail().contains("[invalid_token_response]")) {
				LOG.error("Messaging indicates that the certificate to the external digital mail provider in digital-mail-sender-service is invalid");
				throw e;
			}
			return handleNotSent(e);

		} catch (final Exception e) {
			return handleNotSent(e);
		}
	}

	private ItemStatus handleNotSent(Exception e) {
		LOG.warn("Unable to send invoice", e);

		return NOT_SENT;
	}

	public void sendErrorReport(final LocalDate date, final String municipalityId, String batchName, String message) {
		LOG.info("Sending error report");
		final var request = messagingMapper.toErrorEmailRequest(generateErrorReportMessage(municipalityId, batchName, message), date);

		for (final var recipientEmailAddress : properties.errorReport().recipientEmailAddresses()) {
			try {
				request.setEmailAddress(recipientEmailAddress);
				client.sendEmail(municipalityId, request);
				LOG.info("Error report sent to {}", recipientEmailAddress);
			} catch (final Exception e) {
				LOG.warn("Unable to send error report to {}", recipientEmailAddress, e);
			}
		}

	}

	public void sendStatusReport(final List<BatchEntity> batches, final LocalDate date, final String municipalityId) {
		LOG.info("Sending status report");
		final var request = messagingMapper.toStatusEmailRequest(generateStatusReportMessage(batches), date);

		for (final var recipientEmailAddress : properties.statusReport().recipientEmailAddresses()) {
			try {
				request.setEmailAddress(recipientEmailAddress);

				client.sendEmail(municipalityId, request);
				LOG.info(LOG_MESSAGE_STATUS_REPORT, recipientEmailAddress);
			} catch (final Exception e) {
				LOG.warn("Unable to send status report to {}", recipientEmailAddress, e);
			}
		}
	}

	String generateErrorReportMessage(final String municipalityId, final String batchName, final String message) {
		final var context = new Context();
		context.setVariable("requestId", RequestId.get());
		context.setVariable("municipalityId", municipalityId);
		context.setVariable("batchName", batchName);
		context.setVariable("message", message);
		final var htmlMessage = templateEngine.process(ERROR_TEMPLATE_NAME, context);
		return Base64.getEncoder().encodeToString(htmlMessage.getBytes(UTF_8));
	}

	String generateStatusReportMessage(final List<BatchEntity> batches) {
		final var context = new Context();
		context.setVariable("batches", batches);
		final var htmlMessage = templateEngine.process(STATUS_TEMPLATE_NAME, context);
		return Base64.getEncoder().encodeToString(htmlMessage.getBytes(UTF_8));
	}

	public void sendSlackMessage(final String municipalityId, final String message) {
		LOG.info("Sending '{}' as slack message", message);

		final var request = messagingMapper.toSlackRequest(message);

		try {
			client.sendSlackMessage(municipalityId, request);
			LOG.info(LOG_MESSAGE_STATUS_REPORT, request.getChannel());
		} catch (final Exception e) {
			LOG.warn("Unable to send slack message to {}", request.getChannel());
		}
	}

	public void sendSlackMessage(final BatchEntity batch, final LocalDate date, final String municipalityId) {
		sendSlackMessage(municipalityId, generateSlackMessage(batch, date));
	}

	String generateSlackMessage(final BatchEntity batch, LocalDate date) {
		final var numberOfSentInvoices = batch.getSentItems();
		final var numberOfNotSentInvoices = batch.getItems().stream()
			.filter(invoices -> invoices.getType() == INVOICE)
			.filter(invoices -> invoices.getStatus() != SENT)
			.count();

		return """
			Batch: %s
			Date: %s
			Invoices sent: %s
			Invoices not sent: %s
			""".formatted(batch.getBasename(), date, numberOfSentInvoices, numberOfNotSentInvoices);
	}
}
