package se.sundsvall.invoicesender.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PersonUtilTests {

    @ParameterizedTest
    @MethodSource("argumentsForAddCenturyDigitsToLegalId")
    void addCenturyDigitsToLegalId(final String legalId, final String expected) {
        assertThat(PersonUtil.addCenturyDigitsToLegalId(legalId)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("argumentsForIsValidLegalId")
    void isValidLegalId(final String legalId, final boolean valid) {
        assertThat(PersonUtil.isValidLegalId(legalId)).isEqualTo(valid);
    }

    static Stream<Arguments> argumentsForAddCenturyDigitsToLegalId() {
        return Stream.of(
            Arguments.of("195505158888", "195505158888"),
            Arguments.of("200506071234", "200506071234"),
            Arguments.of("5505158888", "195505158888"),
            Arguments.of("0506071234", "200506071234")
        );
    }

    static Stream<Arguments> argumentsForIsValidLegalId() {
        return Stream.of(
            Arguments.of("not-containing-digits", false),   // No digits at all
            Arguments.of("12345", false),                   // Too short
            Arguments.of("12345671234567", false),          // Too long
            Arguments.of("195513071770", false),            // Invalid date part with 13 as the month field
            Arguments.of("195513071770", false),            // Invalid date part with 13 as the month field
            Arguments.of("8701162383", true),               // Valid 10-digit legal id
            Arguments.of("950211-2387", true),              // Valid 10-digit legal id with dash
            Arguments.of("198701162383", true),             // Valid 12-digit legal id
            Arguments.of("19820906-2390", true),            // Valid 12-digit legal id with dash
            Arguments.of("198701162382", false)             // Invalid - wrong check digit
        );
    }
}
