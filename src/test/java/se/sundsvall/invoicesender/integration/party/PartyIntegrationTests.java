package se.sundsvall.invoicesender.integration.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.party.PartyType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PartyIntegrationTests {

	@Mock
	private PartyClient mockPartyClient;

	@InjectMocks
	private PartyIntegration partyIntegration;

	@Test
	void testGetPartyId() {
		when(mockPartyClient.getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class)))
			.thenReturn(Optional.of("somePartyId"));

		final var partyId = partyIntegration.getPartyId("5505158888", "2281");

		assertThat(partyId).hasValue("somePartyId");

		verify(mockPartyClient, times(1)).getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	@Test
	void testGetPartyIdWhenNothingIsFound() {
		when(mockPartyClient.getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class)))
			.thenReturn(Optional.empty());

		final var partyId = partyIntegration.getPartyId("5505158888", "2281");

		assertThat(partyId).isEmpty();

		verify(mockPartyClient, times(1)).getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	@Test
	void testGetPartyIdWhenExceptionIsThrown() {
		when(mockPartyClient.getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class)))
			.thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

		final var partyId = partyIntegration.getPartyId("5505158888", "2281");

		assertThat(partyId).isEmpty();

		verify(mockPartyClient, times(1)).getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}
}
