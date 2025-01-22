package se.sundsvall.invoicesender.integration.citizen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CitizenIntegrationTests {

	@Mock
	private CitizenClient mockCitizenClient;
	@Mock
	private ResponseEntity<Void> mockPersonIdResponse;

	@InjectMocks
	private CitizenIntegration citizenIntegration;

	@Test
	void hasProtectedIdentity() {
		var partyId1 = "personId1";
		var partyId2 = "personId2";

		when(mockPersonIdResponse.getStatusCode()).thenReturn(NO_CONTENT).thenReturn(OK);
		when(mockCitizenClient.getPerson(partyId1)).thenReturn(mockPersonIdResponse);
		when(mockCitizenClient.getPerson(partyId2)).thenReturn(mockPersonIdResponse);

		assertThat(citizenIntegration.hasProtectedIdentity(partyId1)).isTrue();
		assertThat(citizenIntegration.hasProtectedIdentity(partyId2)).isFalse();

		verify(mockCitizenClient).getPerson(partyId1);
		verify(mockCitizenClient).getPerson(partyId2);
		verify(mockPersonIdResponse, times(2)).getStatusCode();
		verifyNoMoreInteractions(mockCitizenClient, mockPersonIdResponse);
	}

	@Test
	void hasProtectedIdentityWhenCitizenClientThrowsException() {
		var partyId = "partyId";

		when(mockCitizenClient.getPerson(partyId)).thenThrow(new NullPointerException());

		assertThat(citizenIntegration.hasProtectedIdentity(partyId)).isFalse();

		verify(mockCitizenClient).getPerson(partyId);
		verifyNoMoreInteractions(mockCitizenClient);
	}
}
