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

	/**
	 * Get party id for a legal id. The legal id is expected to be a personal number.
	 *
	 * @param  legalId        the legal id to search for
	 * @param  municipalityId the municipality id
	 * @return                Optional of LegalIdAndPartyId which is a key-value pair of legal id and party id.
	 */
	public Optional<LegalIdAndPartyId> getPartyId(final String legalId, final String municipalityId) {
		// Strip everything but digits from the legal id
		var legalIdWithDigitsOnly = legalId.replaceAll("\\D", "");
		String legalIdWithCentury = "";

		try {
			legalIdWithCentury = guessLegalIdCenturyDigits(legalIdWithDigitsOnly);
			var finalLegalId = legalIdWithCentury;
			return partyClient.getPartyId(municipalityId, PRIVATE, legalIdWithCentury).map(partyId -> new LegalIdAndPartyId(finalLegalId, partyId));
		} catch (final Exception e) {
			LOG.info("Unable to get party id for legal id: {} (calculated to {}), {}", legalIdWithDigitsOnly, legalIdWithCentury, e.getMessage());
			return Optional.empty();
		}
	}
}
