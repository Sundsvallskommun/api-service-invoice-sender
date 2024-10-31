package se.sundsvall.invoicesender.integration.citizen;

import static org.apache.commons.lang3.StringUtils.strip;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static se.sundsvall.invoicesender.util.LegalIdUtil.addCenturyDigitsToLegalId;

import org.springframework.stereotype.Component;

@Component
public class CitizenIntegration {

	static final String INTEGRATION_NAME = "citizen";

	private final CitizenClient citizenClient;

	CitizenIntegration(final CitizenClient citizenClient) {
		this.citizenClient = citizenClient;
	}

	public boolean hasProtectedIdentity(final String personalNumber) {
		var personalNumberWithCenturyDigits = addCenturyDigitsToLegalId(personalNumber);

		try {
			// Get the person id
			var personId = citizenClient.getPersonId(personalNumberWithCenturyDigits);
			// Remove the quotation marks that exist for whatever reason from the person id
			var cleanPersonId = strip(personId, "\"");
			// Get the person data, or rather just the HTTP status code for it - a request for data for
			// a protected identity person yields a 204 No Content
			var personResponse = citizenClient.getPerson(cleanPersonId);

			return personResponse.getStatusCode().isSameCodeAs(NO_CONTENT);
		} catch (Exception e) {
			// If anything goes wrong - assume that the recipient doesn't have a protected identity
			return false;
		}
	}
}
