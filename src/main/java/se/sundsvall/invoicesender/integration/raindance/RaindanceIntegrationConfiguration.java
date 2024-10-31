package se.sundsvall.invoicesender.integration.raindance;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RaindanceIntegrationProperties.class)
class RaindanceIntegrationConfiguration {

}
