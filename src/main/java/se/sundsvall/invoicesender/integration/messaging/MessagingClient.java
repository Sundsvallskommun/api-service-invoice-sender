package se.sundsvall.invoicesender.integration.messaging;

import static se.sundsvall.invoicesender.integration.messaging.MessagingIntegrationConfiguration.INTEGRATION_NAME;

import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
	name = INTEGRATION_NAME,
	configuration = MessagingIntegrationConfiguration.class,
	url = "${integration.messaging.url}")
@CircuitBreaker(name = INTEGRATION_NAME)
interface MessagingClient {

	@PostMapping("/{municipalityId}/digital-invoice")
	MessageResult sendDigitalInvoice(@PathVariable("municipalityId") String municipalityId, @RequestBody DigitalInvoiceRequest request);

	@PostMapping("/{municipalityId}/email")
	MessageResult sendEmail(@PathVariable("municipalityId") String municipalityId, @RequestBody EmailRequest request);

}
