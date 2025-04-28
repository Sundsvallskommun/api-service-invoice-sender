package se.sundsvall.invoicesender.integration.party;

import static generated.se.sundsvall.party.PartyType.PRIVATE;
import static se.sundsvall.invoicesender.util.LegalIdUtil.guessLegalIdCenturyDigits;

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

	public Optional<LegalIdAndPartyId> getPartyId(final String legalId, final String municipalityId) {
		// Strip everything but digits from the legal id
		var legalIdWithDigitsOnly = legalId.replaceAll("\\D", "");

		try {
			var legalIdWithBirthYear = guessLegalIdCenturyDigits(legalIdWithDigitsOnly);
			return partyClient.getPartyId(municipalityId, PRIVATE, legalIdWithBirthYear).map(partyId -> new LegalIdAndPartyId(legalIdWithBirthYear, partyId));
		} catch (final Exception e) {
			LOG.info("Unable to get party id for legal id: {}, {}", legalIdWithDigitsOnly, e.getMessage());
			return Optional.empty();
		}
	}
}
