package se.sundsvall.invoicesender.integration.citizen;

import static org.apache.commons.lang3.StringUtils.strip;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.springframework.stereotype.Component;

@Component
public class CitizenIntegration {

	static final String INTEGRATION_NAME = "citizen";

	private final CitizenClient citizenClient;

	CitizenIntegration(final CitizenClient citizenClient) {
		this.citizenClient = citizenClient;
	}

	public boolean hasProtectedIdentity(final String partyId) {
		try {
			var cleanPartyId = strip(partyId, "\"");
			// Get the person data, or rather just the HTTP status code for it - a request for data for
			// a protected identity person yields a 204 No Content
			var personResponse = citizenClient.getPerson(cleanPartyId);

			return personResponse.getStatusCode().isSameCodeAs(NO_CONTENT);
		} catch (Exception e) {
			// If anything goes wrong - assume that the recipient doesn't have a protected identity
			return false;
		}
	}
}
