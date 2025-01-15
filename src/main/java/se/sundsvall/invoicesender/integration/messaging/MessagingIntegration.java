package se.sundsvall.invoicesender.integration.messaging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.SENT;

import generated.se.sundsvall.messaging.MessageStatus;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemStatus;

@Component
public class MessagingIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(MessagingIntegration.class);
	static final String TEMPLATE_NAME = "status-report";

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
			var request = messagingMapper.toDigitalInvoiceRequest(invoice, path);

			var response = client.sendDigitalInvoice(municipalityId, request);

			// We know that we have a single message with a single delivery - extract the status
			var status = response.getDeliveries().getFirst().getStatus();

			return status == MessageStatus.SENT ? SENT : NOT_SENT;
		} catch (final Exception e) {
			LOG.warn("Unable to send invoice", e);

			return NOT_SENT;
		}
	}

	public void sendStatusReport(final List<BatchEntity> batches, final LocalDate date, final String municipalityId) {
		LOG.info("Sending status report");
		var request = messagingMapper.toEmailRequest(generateStatusReportMessage(batches), date);

		for (var recipientEmailAddress : properties.statusReport().recipientEmailAddresses()) {
			try {
				request.setEmailAddress(recipientEmailAddress);

				client.sendEmail(municipalityId, request);
				LOG.info("Status report sent to {}", recipientEmailAddress);
			} catch (final Exception e) {
				LOG.warn("Unable to send status report to {}", recipientEmailAddress, e);
			}
		}
	}

	String generateStatusReportMessage(final List<BatchEntity> batches) {
		var context = new Context();
		context.setVariable("batches", batches);
		var htmlMessage = templateEngine.process(TEMPLATE_NAME, context);
		return Base64.getEncoder().encodeToString(htmlMessage.getBytes(UTF_8));
	}

}
