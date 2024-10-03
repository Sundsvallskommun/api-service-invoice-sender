package se.sundsvall.invoicesender.integration.citizen;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CitizenIntegration {

    static final String INTEGRATION_NAME = "citizen";

    private static final Logger LOG = LoggerFactory.getLogger(CitizenIntegration.class);

    private final CitizenClient citizenClient;

    CitizenIntegration(final CitizenClient citizenClient) {
        this.citizenClient = citizenClient;
    }

    public boolean hasProtectedIdentity(final String personalNumber) {
        // Get the person id
        var personId = citizenClient.getPersonId(personalNumber);
        // Get the person data, or rather just the HTTP status code for it - a request for data for
        // a protected identity person yields a 204 No Content
        var personResponse = citizenClient.getPerson(personId);

        return personResponse.getStatusCode().isSameCodeAs(NO_CONTENT);
    }
}
