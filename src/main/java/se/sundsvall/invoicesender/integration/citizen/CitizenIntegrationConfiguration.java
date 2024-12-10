package se.sundsvall.invoicesender.integration.citizen;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.invoicesender.integration.citizen.CitizenIntegration.INTEGRATION_NAME;

import feign.Request;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

@Import(FeignConfiguration.class)
@EnableConfigurationProperties(CitizenIntegrationProperties.class)
class CitizenIntegrationConfiguration {

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final CitizenIntegrationProperties properties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(INTEGRATION_NAME, List.of(NOT_FOUND.value())))
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
