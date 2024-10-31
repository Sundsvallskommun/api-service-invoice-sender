package se.sundsvall.invoicesender.integration;

import jakarta.validation.constraints.NotBlank;

public record Oauth2(

	@NotBlank String tokenUrl,

	@NotBlank String clientId,

	@NotBlank String clientSecret) {}
