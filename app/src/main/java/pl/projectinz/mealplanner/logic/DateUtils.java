package pl.projectinz.mealplanner.logic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// helper methods for dates saved as "yyyy-MM-dd" text
//
// why text and not a timestamp:
// - a meal is planned for a day, the exact time doesn't matter
// - the text sorts like a date, so range queries in Firestore work
// - no problems with time zones
//
// everything is in UTC because the date picker also gives UTC millis
public final class DateUtils {

    public static final String PATTERN = "yyyy-MM-dd";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private DateUtils() {
        // only static methods
    }

    private static SimpleDateFormat createFormat() {
        SimpleDateFormat format = new SimpleDateFormat(PATTERN, Locale.US);
        format.setTimeZone(UTC);
        format.setLenient(false);
        return format;
    }

    // UTC millis (from MaterialDatePicker) -> "yyyy-MM-dd"
    public static String fromUtcMillis(long utcMillis) {
        return createFormat().format(new Date(utcMillis));
    }

    // "yyyy-MM-dd" -> UTC millis (start of that day)
    public static long toUtcMillis(String date) {
        try {
            return createFormat().parse(date).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date: " + date);
        }
    }

    // today's date in the phone's time zone
    public static String today() {
        Calendar local = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH) + 1,
                local.get(Calendar.DAY_OF_MONTH));
    }

    // adds days (or removes if negative), e.g. 2026-12-31 + 1 = 2027-01-01
    public static String addDays(String date, int days) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.setTimeInMillis(toUtcMillis(date));
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return fromUtcMillis(calendar.getTimeInMillis());
    }

    // checks if it's a real date, e.g. 2026-02-30 is not
    public static boolean isValid(String date) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        try {
            createFormat().parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // true if date is between from and to (both included)
    public static boolean isInRange(String date, String from, String to) {
        return date.compareTo(from) >= 0 && date.compareTo(to) <= 0;
    }

    // gives the Monday of the week, e.g. 2026-11-15 (Sunday) -> 2026-11-09
    public static String startOfWeek(String date) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.setTimeInMillis(toUtcMillis(date));
        // Calendar.MONDAY = 2, SUNDAY = 1, so this gives Mon=0 ... Sun=6
        int daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        return addDays(date, -daysSinceMonday);
    }

    // formats the date for the screen, e.g. "Monday, 16 Nov"
    public static String formatForDisplay(String date, String pattern, Locale locale) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        format.setTimeZone(UTC);
        return format.format(new Date(toUtcMillis(date)));
    }
}
