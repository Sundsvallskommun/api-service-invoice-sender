package se.sundsvall.invoicesender.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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

	@ParameterizedTest
	@ValueSource(strings = {
		"1234567890", "1234567890123", "12345678901234567890", "1234567890123456789012345678901234567890"
	})
	void getBirthYear(final String legalId) {

		var birthYear = LegalIdUtil.getBirthYear(legalId);
		assertThat(birthYear).isEqualTo(12);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"abc", "lol", "rofl"
	})
	void getBirthYear_NumberFormatException(final String legalId) {
		assertThatThrownBy(() -> LegalIdUtil.getBirthYear(legalId))
			.isInstanceOf(NumberFormatException.class)
			.hasMessage("For input string: \"" + legalId.substring(0, 2) + "\"");
	}

	@ParameterizedTest
	@NullSource
	void getBirthYear_NullPointerException(final String legalId) {
		assertThatThrownBy(() -> getBirthYear(legalId))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("Cannot invoke \"String.substring(int, int)\" because \"legalIdWithDigitsOnly\" is null");
	}

	@ParameterizedTest
	@EmptySource
	void getBirthYear_IllegalArgumentException(final String legalId) {
		assertThatThrownBy(() -> getBirthYear(legalId))
			.isInstanceOf(StringIndexOutOfBoundsException.class)
			.hasMessage("Range [0, 2) out of bounds for length 0");
	}

	@ParameterizedTest
	@MethodSource("guessLegalIdCenturyDigitsArgumentProvider")
	void guessLegalIdCenturyDigits(final String given, final String expected) {
		var result = LegalIdUtil.guessLegalIdCenturyDigits(given);
		assertThat(result).isEqualTo(expected);
	}

	static Stream<Arguments> guessLegalIdCenturyDigitsArgumentProvider() {
		return Stream.of(
			Arguments.of("1234567890", "201234567890"),
			Arguments.of("2234567890", "202234567890"),
			Arguments.of("3234567890", "193234567890"),
			Arguments.of("4234567890", "194234567890"),
			Arguments.of("5234567890", "195234567890"),
			Arguments.of("6234567890", "196234567890"));
	}
}
