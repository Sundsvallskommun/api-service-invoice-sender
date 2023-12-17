package se.sundsvall.invoicesender.integration.party;

import static generated.se.sundsvall.party.PartyType.PRIVATE;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PartyIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(PartyIntegration.class);

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
            LOG.info("Unable to get party id for legal id {}: {}", legalId, e.getMessage());

            return Optional.empty();
        }
    }
}
