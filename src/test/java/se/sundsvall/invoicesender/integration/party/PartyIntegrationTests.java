package se.sundsvall.invoicesender.integration.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.party.PartyType;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

	@ParameterizedTest
	@MethodSource("getPartyIdArgumentProvider")
	void testGetPartyId(final String givenLegalId, final String expectedLegalId) {
		when(mockPartyClient.getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class)))
			.thenReturn(Optional.of("somePartyId"));

		var legalIdAndPartyId = partyIntegration.getPartyId(givenLegalId, "2281");

		assertThat(legalIdAndPartyId).isPresent().hasValueSatisfying(actualValue -> {
			assertThat(actualValue.legalId()).isEqualTo(expectedLegalId);
			assertThat(actualValue.partyId()).isEqualTo("somePartyId");
		});

		verify(mockPartyClient).getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	static Stream<Arguments> getPartyIdArgumentProvider() {
		return Stream.of(
			Arguments.of("5505158888", "195505158888"),
			Arguments.of("0405158888", "200405158888"));
	}

	@Test
	void testGetPartyIdWhenNothingIsFound() {
		when(mockPartyClient.getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class)))
			.thenReturn(Optional.empty());

		final var partyId = partyIntegration.getPartyId("5505158888", "2281");

		assertThat(partyId).isEmpty();

		verify(mockPartyClient).getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	@Test
	void testGetPartyIdWhenExceptionIsThrown() {
		when(mockPartyClient.getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class)))
			.thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

		final var partyId = partyIntegration.getPartyId("5505158888", "2281");

		assertThat(partyId).isEmpty();

		verify(mockPartyClient).getPartyId(any(String.class), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}
}
