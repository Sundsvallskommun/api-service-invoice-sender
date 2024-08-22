package se.sundsvall.invoicesender.integration.party;

import static generated.se.sundsvall.party.PartyType.PRIVATE;

import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PartyIntegration {

	static final String INTEGRATION_NAME = "Party";

	private static final Logger LOG = LoggerFactory.getLogger(PartyIntegration.class);

	private final PartyClient partyClient;

	PartyIntegration(final PartyClient partyClient) {
		this.partyClient = partyClient;
	}

	public Optional<String> getPartyId(final String legalId, final String municipalityId) {
		// Only handle "PRIVATE" legal id:s for now
		final var legalIdWithCenturyDigits = addCenturyDigitToLegalId(legalId);

		try {
			return partyClient.getPartyId(municipalityId, PRIVATE, legalIdWithCenturyDigits);
		} catch (final Exception e) {
			LOG.info("Unable to get party id for legal id {}: {}", legalIdWithCenturyDigits, e.getMessage());

			return Optional.empty();
		}
	}

	String addCenturyDigitToLegalId(final String legalId) {
		// Make sure we have digits only
		if (!legalId.matches("^\\d+$")) {
			throw new IllegalArgumentException("Invalid legal id: " + legalId);
		}
		// Do nothing if we already have a legal id with century digits
		if (legalId.startsWith("19") || legalId.startsWith("20")) {
			return legalId;
		}

		// Naively validate
		final var thisYear = LocalDate.now().getYear() % 2000;
		final var legalIdYear = Integer.parseInt(legalId.substring(0, 2));

		return (legalIdYear <= thisYear ? "20" : "19") + legalId;
	}

}
