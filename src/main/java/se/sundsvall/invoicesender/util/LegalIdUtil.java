package se.sundsvall.invoicesender.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

public final class LegalIdUtil {

    private static final LuhnCheckDigit LUHN = new LuhnCheckDigit();
    private static final DateTimeFormatter DATE_PART_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private LegalIdUtil() { }

    public static String addCenturyDigitsToLegalId(final String legalId) {
        // Strip everything but digits from the legal id
        var legalIdWithDigitsOnly = legalId.replaceAll("\\D", "");

        // Do nothing if we already have a legal id with century digits
        if (legalIdWithDigitsOnly.startsWith("19") || legalIdWithDigitsOnly.startsWith("20")) {
            return legalIdWithDigitsOnly;
        }

        // Naively validate
        var thisYear = LocalDate.now().getYear() % 2000;
        var legalIdYear = Integer.parseInt(legalIdWithDigitsOnly.substring(0, 2));

        return (legalIdYear <= thisYear ? "20" : "19") + legalIdWithDigitsOnly;
    }

    public static boolean isValidLegalId(final String legalId) {
        try {
            // Strip everything but digits from the legal id
            var legalIdWithDigitsOnly = legalId.replaceAll("\\D", "");

            // Add the century digits if they're missing
            if (legalIdWithDigitsOnly.length() == 10) {
                var thisYear = LocalDate.now().getYear() % 2000;
                var legalIdYear = Integer.parseInt(legalId.substring(0, 2));

                legalIdWithDigitsOnly =  (legalIdYear <= thisYear ? "20" : "19") + legalIdWithDigitsOnly;
            }

            // At this point, we should have a legal id with 12 digits
            if (legalIdWithDigitsOnly.length() != 12) {
                return false;
            }

            // "Validate" the date part (YYYYMMDD) by trying to parse it into a LocalDate
            DATE_PART_FORMAT.parse(legalIdWithDigitsOnly.substring(0, 8));

            // Calculate and validate the check digit
            var checkDigit = LUHN.calculate(legalIdWithDigitsOnly.substring(2, legalIdWithDigitsOnly.length() - 1));
            return checkDigit.equals(legalIdWithDigitsOnly.substring(legalIdWithDigitsOnly.length() - 1));
        } catch (DateTimeParseException | CheckDigitException e) {
            return false;
        }
    }
}
