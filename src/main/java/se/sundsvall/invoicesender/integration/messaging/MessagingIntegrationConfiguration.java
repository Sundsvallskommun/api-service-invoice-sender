package se.sundsvall.invoicesender.integration.messaging;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static se.sundsvall.invoicesender.integration.messaging.MessagingIntegration.INTEGRATION_NAME;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import feign.Request;

@Import(FeignConfiguration.class)
@EnableConfigurationProperties(MessagingIntegrationProperties.class)
class MessagingIntegrationConfiguration {

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final MessagingIntegrationProperties properties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(INTEGRATION_NAME))
			.withRetryableOAuth2InterceptorForClientRegistration(ClientRegistration
				.withRegistrationId(INTEGRATION_NAME)
				.tokenUri(properties.oauth2().tokenUrl())
				.clientId(properties.oauth2().clientId())
				.clientSecret(properties.oauth2().clientSecret())
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build())
			.withRequestOptions(new Request.Options(
				properties.connectTimeout().toMillis(), MILLISECONDS,
				properties.readTimeout().toMillis(), MILLISECONDS,
				true))
			.composeCustomizersToOne();
	}
}
