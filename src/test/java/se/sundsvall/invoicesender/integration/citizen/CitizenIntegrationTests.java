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
		var personalNumber1 = "personalNumber1";
		var personId1 = "personId1";
		var personalNumber2 = "personalNumber2";
		var personId2 = "personId2";

		when(mockPersonIdResponse.getStatusCode()).thenReturn(NO_CONTENT).thenReturn(OK);
		when(mockCitizenClient.getPersonId(personalNumber1)).thenReturn(personId1);
		when(mockCitizenClient.getPerson(personId1)).thenReturn(mockPersonIdResponse);
		when(mockCitizenClient.getPersonId(personalNumber2)).thenReturn(personId2);
		when(mockCitizenClient.getPerson(personId2)).thenReturn(mockPersonIdResponse);

		assertThat(citizenIntegration.hasProtectedIdentity(personalNumber1)).isTrue();
		assertThat(citizenIntegration.hasProtectedIdentity(personalNumber2)).isFalse();

		verify(mockCitizenClient).getPersonId(personalNumber1);
		verify(mockCitizenClient).getPerson(personId1);
		verify(mockCitizenClient).getPersonId(personalNumber2);
		verify(mockCitizenClient).getPerson(personId2);
		verify(mockPersonIdResponse, times(2)).getStatusCode();
		verifyNoMoreInteractions(mockCitizenClient, mockPersonIdResponse);
	}
}
