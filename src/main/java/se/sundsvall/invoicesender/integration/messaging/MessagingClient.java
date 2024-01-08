package se.sundsvall.invoicesender.integration.messaging;

import static se.sundsvall.invoicesender.integration.messaging.MessagingIntegration.INTEGRATION_NAME;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import generated.se.sundsvall.messaging.DigitalInvoiceRequest;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;

@FeignClient(
    name = INTEGRATION_NAME,
    configuration = MessagingIntegrationConfiguration.class,
    url = "${integration.messaging.url}"
)
interface MessagingClient {

    @PostMapping("/digital-invoice")
    MessageResult sendDigitalInvoice(@RequestBody DigitalInvoiceRequest request);

    @PostMapping("/email")
    MessageResult sendEmail(@RequestBody EmailRequest request);
}
