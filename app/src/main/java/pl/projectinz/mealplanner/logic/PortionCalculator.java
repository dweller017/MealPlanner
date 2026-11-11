package pl.projectinz.mealplanner.logic;

// calculates the quantity for a different number of portions
//
// requiredQuantity = ingredientQuantity * selectedPortions / defaultPortions
//
// example: recipe for 2 portions has 200 g chicken,
// for 4 portions it's 200 * 4 / 2 = 400 g
public class PortionCalculator {

    // throws IllegalArgumentException if portions <= 0 or quantity < 0
    public double calculate(double ingredientQuantity, int defaultPortions, int selectedPortions) {
        if (ingredientQuantity < 0) {
            throw new IllegalArgumentException("Ingredient quantity cannot be negative");
        }
        if (defaultPortions <= 0) {
            throw new IllegalArgumentException("Default portions must be greater than 0");
        }
        if (selectedPortions <= 0) {
            throw new IllegalArgumentException("Selected portions must be greater than 0");
        }
        return ingredientQuantity * selectedPortions / defaultPortions;
    }
}
