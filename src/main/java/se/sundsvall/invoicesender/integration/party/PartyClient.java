package se.sundsvall.invoicesender.integration.party;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.invoicesender.integration.party.PartyIntegration.INTEGRATION_NAME;

import generated.se.sundsvall.party.PartyType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
	name = INTEGRATION_NAME,
	configuration = PartyIntegrationConfiguration.class,
	url = "${integration.party.url}",
	dismiss404 = true)
@CircuitBreaker(name = INTEGRATION_NAME)
interface PartyClient {

	@GetMapping(
		path = "/{municipalityId}/{type}/{legalId}/partyId",
		produces = {
			TEXT_PLAIN_VALUE, APPLICATION_PROBLEM_JSON_VALUE
		})
	Optional<String> getPartyId(@PathVariable("municipalityId") String municipalityId, @PathVariable("type") PartyType partyType, @PathVariable("legalId") String legalId);

}
