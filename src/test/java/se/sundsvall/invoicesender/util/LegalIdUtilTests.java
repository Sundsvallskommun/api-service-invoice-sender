package se.sundsvall.invoicesender.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LegalIdUtilTests {

	@ParameterizedTest
	@MethodSource("argumentsForIsValidLegalId")
	void isValidLegalId(final String legalId, final boolean valid, final String description) {
		assertThat(LegalIdUtil.isValidLegalId(legalId)).as(description).isEqualTo(valid);
	}

	static Stream<Arguments> argumentsForIsValidLegalId() {
		return Stream.of(
			Arguments.of("not-containing-digits", false, "No digits at all"),   // No digits at all
			Arguments.of("12345", false, "Too short"),                   // Too short
			Arguments.of("12345671234567", false, "Too long"),          // Too long
			Arguments.of("5513071770", false, "Invalid date part - 13 as the month field"),            // Invalid date part with 13 as the month field
			Arguments.of("8701162383", true, "Valid 10-digit legal id"),               // Valid 10-digit legal id
			Arguments.of("950211-2387", true, "Valid 10-digit legal id with dash"),              // Valid 10-digit legal id with dash
			Arguments.of("8701162382", false, "Invalid check-digit")             // Invalid - wrong check digit
		);
	}
}
