package se.sundsvall.invoicesender.integration.citizen;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.invoicesender.integration.citizen.CitizenIntegration.INTEGRATION_NAME;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
	name = INTEGRATION_NAME,
	configuration = CitizenIntegrationConfiguration.class,
	url = "${integration.citizen.url}")
@CircuitBreaker(name = INTEGRATION_NAME)
interface CitizenClient {

	@GetMapping(path = "/{personalNumber}/guid", produces = TEXT_PLAIN_VALUE)
	String getPersonId(@PathVariable("personalNumber") String personalNumber);

	@GetMapping(path = "/{personId}", produces = ALL_VALUE)
	ResponseEntity<Void> getPerson(@PathVariable("personId") String personId);
}
