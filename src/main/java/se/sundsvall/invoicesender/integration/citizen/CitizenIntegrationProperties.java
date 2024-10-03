package se.sundsvall.invoicesender.integration.citizen;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import se.sundsvall.invoicesender.integration.Oauth2;

@ConfigurationProperties(prefix = "integration.citizen")
record CitizenIntegrationProperties(

    @NotBlank
    String url,

    @DefaultValue("PT10S")
    Duration connectTimeout,

    @DefaultValue("PT30S")
    Duration readTimeout,

    @Valid
    @NotNull
    Oauth2 oauth2) { }
