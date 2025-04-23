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

	public Optional<LegalIdAndPartyId> getPartyId(final String legalId, final String municipalityId) {
		// Strip everything but digits from the legal id
		var legalIdWithDigitsOnly = legalId.replaceAll("\\D", "");

		var currentYear = LocalDate.now().getYear();
		// Example legal id : 900101-1234. Birth year is the first two digits of the legal id.
		var birthYear = Integer.parseInt(legalIdWithDigitsOnly.substring(0, 2));

		// Example currentYear : 2023. 2023 % 100 = 23. If birthYear is greater than currentYear % 100, then the birth year is
		// in the 1900s.
		// This solution is flawed for people older than 100.
		String legalIdWithBirthYear;
		if (birthYear > currentYear % 100) {
			legalIdWithBirthYear = "19".concat(legalIdWithDigitsOnly);
		} else {
			legalIdWithBirthYear = "20".concat(legalIdWithDigitsOnly);
		}

		try {
			return partyClient.getPartyId(municipalityId, PRIVATE, legalIdWithBirthYear).map(partyId -> new LegalIdAndPartyId(legalIdWithBirthYear, partyId));
		} catch (final Exception e) {
			LOG.info("Unable to get party id for legal id {}: {}", legalIdWithBirthYear, e.getMessage());
			return Optional.empty();
		}
	}
}
