package se.sundsvall.invoicesender.integration.party;

import static generated.se.sundsvall.party.PartyType.PRIVATE;
import static se.sundsvall.invoicesender.util.PersonUtil.addCenturyDigitsToLegalId;

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
		var legalIdWithCenturyDigits = addCenturyDigitsToLegalId(legalId);

		try {
			return partyClient.getPartyId(municipalityId, PRIVATE, legalIdWithCenturyDigits);
		} catch (final Exception e) {
			LOG.info("Unable to get party id for legal id {}: {}", legalIdWithCenturyDigits, e.getMessage());

			return Optional.empty();
		}
	}
}
