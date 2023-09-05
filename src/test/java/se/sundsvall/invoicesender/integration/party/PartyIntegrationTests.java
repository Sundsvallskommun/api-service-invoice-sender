package se.sundsvall.invoicesender.integration.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import generated.se.sundsvall.party.PartyType;

@ExtendWith(MockitoExtension.class)
class PartyIntegrationTests {

    @Mock
    private PartyClient mockPartyClient;
    @InjectMocks
    private PartyIntegration partyIntegration;

    @Test
    void testGetPartyId() {
        when(mockPartyClient.getPartyId(eq(PartyType.PRIVATE), any(String.class)))
            .thenReturn(Optional.of("somePartyId"));

        var partyId = partyIntegration.getPartyId("someLegalId");

        assertThat(partyId).hasValue("somePartyId");

        verify(mockPartyClient, times(1)).getPartyId(eq(PartyType.PRIVATE), any(String.class));
        verifyNoMoreInteractions(mockPartyClient);
    }

    @Test
    void testGetPartyIdWhenNothingIsFound() {
        when(mockPartyClient.getPartyId(eq(PartyType.PRIVATE), any(String.class)))
            .thenReturn(Optional.empty());

        var partyId = partyIntegration.getPartyId("someLegalId");

        assertThat(partyId).isEmpty();

        verify(mockPartyClient, times(1)).getPartyId(eq(PartyType.PRIVATE), any(String.class));
        verifyNoMoreInteractions(mockPartyClient);
    }

    @Test
    void testGetPartyIdWhenExceptionIsThrown() {
        when(mockPartyClient.getPartyId(eq(PartyType.PRIVATE), any(String.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        var partyId = partyIntegration.getPartyId("someLegalId");

        assertThat(partyId).isEmpty();

        verify(mockPartyClient, times(1)).getPartyId(eq(PartyType.PRIVATE), any(String.class));
        verifyNoMoreInteractions(mockPartyClient);
    }
}
