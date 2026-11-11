package pl.projectinz.mealplanner.logic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PortionCalculatorTest {

    private static final double DELTA = 0.0001;
    private final PortionCalculator calculator = new PortionCalculator();

    @Test
    public void doublesQuantityWhenPortionsAreDoubled() {
        // recipe for 2 portions: 200 g chicken, requested 4 portions -> 400 g
        assertEquals(400.0, calculator.calculate(200, 2, 4), DELTA);
    }

    @Test
    public void keepsQuantityForTheSamePortions() {
        assertEquals(100.0, calculator.calculate(100, 2, 2), DELTA);
    }

    @Test
    public void reducesQuantityForFewerPortions() {
        // recipe for 4 portions: 1 kg, requested 1 portion -> 0.25 kg
        assertEquals(0.25, calculator.calculate(1, 4, 1), DELTA);
    }

    @Test
    public void worksWithDecimalQuantities() {
        // 1.5 l for 3 portions, requested 2 -> 1.0 l
        assertEquals(1.0, calculator.calculate(1.5, 3, 2), DELTA);
    }

    @Test
    public void handlesNonIntegerResults() {
        // 100 g for 3 portions, requested 1 -> 33.333 g
        assertEquals(33.3333, calculator.calculate(100, 3, 1), DELTA);
    }

    @Test
    public void zeroQuantityStaysZero() {
        assertEquals(0.0, calculator.calculate(0, 2, 5), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroDefaultPortions() {
        calculator.calculate(200, 0, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroSelectedPortions() {
        calculator.calculate(200, 2, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeSelectedPortions() {
        calculator.calculate(200, 2, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeQuantity() {
        calculator.calculate(-5, 2, 2);
    }
}
