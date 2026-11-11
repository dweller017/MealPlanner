package pl.projectinz.mealplanner.logic;

import java.util.regex.Pattern;

// checks and parses what the user typed
// no Android code here so it can be tested with JUnit
// the screen decides what error message to show
public class InputValidator {

    // Firebase needs at least 6 characters
    public static final int MIN_PASSWORD_LENGTH = 6;

    // limits so typos like 2000 portions are not accepted
    public static final int MAX_PORTIONS = 99;
    public static final double MAX_QUANTITY = 100000;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    // whole number > 0, returns null if it's wrong
    public Integer parsePositiveInt(String text) {
        if (!isNotBlank(text)) {
            return null;
        }
        try {
            int value = Integer.parseInt(text.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // decimal number > 0, returns null if it's wrong
    // "1,5" also works because Polish keyboards use a comma
    public Double parsePositiveDecimal(String text) {
        if (!isNotBlank(text)) {
            return null;
        }
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.'));
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // portions: 1 to MAX_PORTIONS, null if wrong
    public Integer parsePortions(String text) {
        Integer value = parsePositiveInt(text);
        return value != null && value <= MAX_PORTIONS ? value : null;
    }

    // quantity: more than 0 and max MAX_QUANTITY, null if wrong
    public Double parseQuantity(String text) {
        Double value = parsePositiveDecimal(text);
        return value != null && value <= MAX_QUANTITY ? value : null;
    }
}
