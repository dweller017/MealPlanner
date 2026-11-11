package pl.projectinz.mealplanner.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    @Test
    public void blankTextIsRejected() {
        assertFalse(validator.isNotBlank(null));
        assertFalse(validator.isNotBlank(""));
        assertFalse(validator.isNotBlank("   "));
        assertTrue(validator.isNotBlank("Pancakes"));
    }

    @Test
    public void validatesEmail() {
        assertTrue(validator.isValidEmail("student@uni.edu.pl"));
        assertTrue(validator.isValidEmail("  a.b@example.com "));
        assertFalse(validator.isValidEmail("student"));
        assertFalse(validator.isValidEmail("student@"));
        assertFalse(validator.isValidEmail(null));
    }

    @Test
    public void validatesPasswordLength() {
        assertFalse(validator.isValidPassword("12345"));
        assertTrue(validator.isValidPassword("123456"));
        assertFalse(validator.isValidPassword(null));
    }

    @Test
    public void parsesPositivePortions() {
        assertEquals(Integer.valueOf(4), validator.parsePositiveInt("4"));
        assertEquals(Integer.valueOf(2), validator.parsePositiveInt(" 2 "));
    }

    @Test
    public void rejectsInvalidPortions() {
        assertNull(validator.parsePositiveInt("0"));
        assertNull(validator.parsePositiveInt("-3"));
        assertNull(validator.parsePositiveInt("2.5"));
        assertNull(validator.parsePositiveInt("abc"));
        assertNull(validator.parsePositiveInt(""));
    }

    @Test
    public void parsesDecimalWithDotOrComma() {
        assertEquals(1.5, validator.parsePositiveDecimal("1.5"), 0.0001);
        assertEquals(1.5, validator.parsePositiveDecimal("1,5"), 0.0001);
        assertEquals(200.0, validator.parsePositiveDecimal("200"), 0.0001);
    }

    @Test
    public void portionsMustBeBetweenOneAndMax() {
        assertEquals(Integer.valueOf(1), validator.parsePortions("1"));
        assertEquals(Integer.valueOf(99), validator.parsePortions("99"));
        assertNull(validator.parsePortions("100"));
        assertNull(validator.parsePortions("0"));
    }

    @Test
    public void quantityMustNotExceedMax() {
        assertEquals(100000.0, validator.parseQuantity("100000"), 0.0001);
        assertEquals(0.5, validator.parseQuantity("0,5"), 0.0001);
        assertNull(validator.parseQuantity("100000.1"));
        assertNull(validator.parseQuantity("0"));
    }

    @Test
    public void rejectsInvalidQuantities() {
        assertNull(validator.parsePositiveDecimal("0"));
        assertNull(validator.parsePositiveDecimal("-1"));
        assertNull(validator.parsePositiveDecimal("abc"));
        assertNull(validator.parsePositiveDecimal(""));
        assertNull(validator.parsePositiveDecimal(null));
        assertNull(validator.parsePositiveDecimal("NaN"));
    }
}
