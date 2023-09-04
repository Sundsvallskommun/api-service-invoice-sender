package se.sundsvall.invoicesender.integration.party;

import static generated.se.sundsvall.party.PartyType.PRIVATE;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class PartyIntegration {

    static final String INTEGRATION_NAME = "Party";

    private final PartyClient partyClient;

    PartyIntegration(final PartyClient partyClient) {
        this.partyClient = partyClient;
    }

    public Optional<String> getPartyId(final String legalId) {
        try {
            // Only handle "PRIVATE" legal id:s for now
            return partyClient.getPartyId(PRIVATE, legalId);
        } catch (Exception e) {
e.printStackTrace(System.err); // TODO: remove...
            return Optional.empty();
        }
    }
}
