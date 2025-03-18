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
		var municipalityId = "municipalityId";

		when(mockPersonIdResponse.getStatusCode()).thenReturn(NO_CONTENT).thenReturn(OK);
		when(mockCitizenClient.getPerson(municipalityId, partyId1)).thenReturn(mockPersonIdResponse);
		when(mockCitizenClient.getPerson(municipalityId, partyId2)).thenReturn(mockPersonIdResponse);

		assertThat(citizenIntegration.hasProtectedIdentity(partyId1, municipalityId)).isTrue();
		assertThat(citizenIntegration.hasProtectedIdentity(partyId2, municipalityId)).isFalse();

		verify(mockCitizenClient).getPerson(municipalityId, partyId1);
		verify(mockCitizenClient).getPerson(municipalityId, partyId2);
		verify(mockPersonIdResponse, times(2)).getStatusCode();
		verifyNoMoreInteractions(mockCitizenClient, mockPersonIdResponse);
	}

	@Test
	void hasProtectedIdentityWhenCitizenClientThrowsException() {
		var partyId = "partyId";
		var municipalityId = "municipalityId";

		when(mockCitizenClient.getPerson(municipalityId, partyId)).thenThrow(new NullPointerException());

		assertThat(citizenIntegration.hasProtectedIdentity(partyId, municipalityId)).isFalse();

		verify(mockCitizenClient).getPerson(municipalityId, partyId);
		verifyNoMoreInteractions(mockCitizenClient);
	}
}
