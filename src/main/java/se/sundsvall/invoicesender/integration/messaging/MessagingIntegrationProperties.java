package se.sundsvall.invoicesender.integration.messaging;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import se.sundsvall.invoicesender.integration.Oauth2;

@Validated
@ConfigurationProperties("integration.messaging")
record MessagingIntegrationProperties(

	@NotBlank String channel,

	@NotBlank String token,

	@NotBlank String url,

	@DefaultValue("PT10S") Duration connectTimeout,

	@DefaultValue("PT30S") Duration readTimeout,

	@Valid @NotNull Oauth2 oauth2,

	@Valid @NotNull Invoice invoice,

	@Valid @NotNull ErrorReport errorReport,

	@Valid @NotNull StatusReport statusReport) {

	record Invoice(

		@NotBlank String subject,

		@DefaultValue("Faktura #") String referencePrefix) {
	}

	record StatusReport(

		@NotBlank String senderName,

		@Email @DefaultValue("noreply@sundsvall.se") String senderEmailAddress,

		@NotEmpty List<@Email @NotBlank String> recipientEmailAddresses,

		@DefaultValue("Utskick av fakturor") String subjectPrefix) {
	}

	record ErrorReport(

		@NotBlank String senderName,

		@Email @DefaultValue("noreply@sundsvall.se") String senderEmailAddress,

		@NotEmpty List<@Email @NotBlank String> recipientEmailAddresses,

		@DefaultValue("Kritiskt fel vid exekvering") String subjectPrefix) {
	}
}
