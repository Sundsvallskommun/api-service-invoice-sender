package se.sundsvall.invoicesender.util;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

public final class LegalIdUtil {

	private static final LuhnCheckDigit LUHN = new LuhnCheckDigit();
	private static final DateTimeFormatter DATE_PART_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private LegalIdUtil() {}

	public static boolean isValidLegalId(final String legalId) {
		// Strip everything but digits from the legal id
		var legalIdWithDigitsOnly = legalId.replaceAll("\\D", "");

		// We should have 10 digits
		if (legalIdWithDigitsOnly.length() != 10) {
			return false;
		}

		return Stream.of("19", "20")
			.map(centuryDigit -> centuryDigit + legalIdWithDigitsOnly)
			.anyMatch(legalIdWithCenturyDigits -> validateLegalIdDatePart(legalIdWithCenturyDigits) && validateLegalIdCheckDigit(legalIdWithCenturyDigits));
	}

	static boolean validateLegalIdDatePart(final String legalId) {
		try {
			// "Validate" the date part (YYYYMMDD) by trying to parse it into a LocalDate
			DATE_PART_FORMAT.parse(legalId.substring(0, 8));

			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	static boolean validateLegalIdCheckDigit(final String legalId) {
		try {
			// Calculate and validate the check digit
			var checkDigit = LUHN.calculate(legalId.substring(2, legalId.length() - 1));
			return checkDigit.equals(legalId.substring(legalId.length() - 1));
		} catch (CheckDigitException e) {
			return false;
		}
	}
}
