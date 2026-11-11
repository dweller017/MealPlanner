package pl.projectinz.mealplanner.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

public class FormatterAndDateUtilsTest {

    private final QuantityFormatter formatter = new QuantityFormatter();

    @Test
    public void formatsWholeNumbersWithoutDecimals() {
        assertEquals("200", formatter.format(200.0, Locale.ENGLISH));
    }

    @Test
    public void formatsDecimalsWithLocaleSeparator() {
        assertEquals("1.5", formatter.format(1.5, Locale.ENGLISH));
        assertEquals("1,5", formatter.format(1.5, Locale.forLanguageTag("pl-PL")));
    }

    @Test
    public void formatsAtMostTwoDecimals() {
        assertEquals("33.33", formatter.format(33.3333, Locale.ENGLISH));
    }

    @Test
    public void convertsUtcMillisToDateAndBack() {
        long millis = DateUtils.toUtcMillis("2026-11-15");
        assertEquals("2026-11-15", DateUtils.fromUtcMillis(millis));
    }

    @Test
    public void addsDaysAcrossMonthAndYear() {
        assertEquals("2027-01-01", DateUtils.addDays("2026-12-31", 1));
        assertEquals("2026-02-28", DateUtils.addDays("2026-03-01", -1));
    }

    @Test
    public void validatesDates() {
        assertTrue(DateUtils.isValid("2026-11-15"));
        assertFalse(DateUtils.isValid("2026-02-30"));
        assertFalse(DateUtils.isValid("15.11.2026"));
        assertFalse(DateUtils.isValid(null));
    }

    @Test
    public void findsMondayOfWeek() {
        assertEquals("2026-11-09", DateUtils.startOfWeek("2026-11-09")); // Monday
        assertEquals("2026-11-09", DateUtils.startOfWeek("2026-11-12")); // Thursday
        assertEquals("2026-11-09", DateUtils.startOfWeek("2026-11-15")); // Sunday
        assertEquals("2026-12-28", DateUtils.startOfWeek("2027-01-01")); // across a year
    }

    @Test
    public void formatsDateForDisplay() {
        assertEquals("Monday, 16 Nov", DateUtils.formatForDisplay("2026-11-16", "EEEE, d MMM", Locale.ENGLISH));
    }

    @Test
    public void checksDateRangeInclusive() {
        assertTrue(DateUtils.isInRange("2026-11-15", "2026-11-15", "2026-11-20"));
        assertTrue(DateUtils.isInRange("2026-11-20", "2026-11-15", "2026-11-20"));
        assertFalse(DateUtils.isInRange("2026-11-21", "2026-11-15", "2026-11-20"));
    }
}
