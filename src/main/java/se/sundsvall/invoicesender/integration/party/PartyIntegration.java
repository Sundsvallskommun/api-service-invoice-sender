package se.sundsvall.invoicesender.integration.party;

import static generated.se.sundsvall.party.PartyType.PRIVATE;

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
			// Use the stripped legal id as-is, if it already starts with 19 or 20
			if (legalIdWithDigitsOnly.startsWith("19") || legalIdWithDigitsOnly.startsWith("20")) {
				return partyClient.getPartyId(municipalityId, PRIVATE, legalIdWithDigitsOnly).map(partyId -> new LegalIdAndPartyId(legalIdWithDigitsOnly, partyId));
			}

			return partyClient.getPartyId(municipalityId, PRIVATE, "19" + legalIdWithDigitsOnly)
				.map(partyId -> new LegalIdAndPartyId("19" + legalIdWithDigitsOnly, partyId))
				.or(() -> partyClient.getPartyId(municipalityId, PRIVATE, "20" + legalIdWithDigitsOnly)
					.map(partyId -> new LegalIdAndPartyId("20" + legalId, partyId)));
		} catch (final Exception e) {
			LOG.info("Unable to get party id for legal id [19|20]{}: {}", legalIdWithDigitsOnly, e.getMessage());

			return Optional.empty();
		}
	}
}
