package se.sundsvall.invoicesender.integration.messaging;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import se.sundsvall.invoicesender.integration.Oauth2;

@ConfigurationProperties("integration.messaging")
record MessagingIntegrationProperties(

        @NotBlank
        String url,

        @DefaultValue("PT10S")
        Duration connectTimeout,

        @DefaultValue("PT30S")
        Duration readTimeout,

        @Valid
        @NotNull
        Oauth2 oauth2,

        @Valid
        @NotNull
        Invoice invoice,

        @Valid
        @NotNull
        StatusReport statusReport) {

    record Invoice(

        @NotBlank
        String subject,

        @DefaultValue("Faktura #")
        String referencePrefix) { }

    record StatusReport(

        @NotBlank
        String senderName,

        @Email
        @DefaultValue("noreply@sundsvall.se")
        String senderEmailAddress,

        @Email
        @NotBlank
        String recipientEmailAddress,

        @DefaultValue("Utskick av fakturor")
        String subjectPrefix) { }
}
