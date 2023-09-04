package se.sundsvall.invoicesender.integration.messaging;

import static se.sundsvall.invoicesender.integration.messaging.MessagingIntegration.INTEGRATION_NAME;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;

@FeignClient(
    name = INTEGRATION_NAME,
    configuration = MessagingIntegrationConfiguration.class,
    url = "${integration.messaging.url}"
)
interface MessagingClient {

    @PostMapping("/digital-mail")
    MessageBatchResult sendDigitalMail(@RequestBody DigitalMailRequest request);
}
