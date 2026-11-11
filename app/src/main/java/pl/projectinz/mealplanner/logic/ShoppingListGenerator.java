package pl.projectinz.mealplanner.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pl.projectinz.mealplanner.model.Ingredient;
import pl.projectinz.mealplanner.model.ItemSource;
import pl.projectinz.mealplanner.model.MeasurementUnit;
import pl.projectinz.mealplanner.model.PlannedMeal;
import pl.projectinz.mealplanner.model.Recipe;
import pl.projectinz.mealplanner.model.ShoppingListItem;
import pl.projectinz.mealplanner.model.UnitDimension;

// makes one shopping list from the planned meals
//
// how it works:
// 1. find the recipe of each planned meal (skip the meal if the recipe is gone)
// 2. recalculate every ingredient for the planned portions
// 3. change it to the base unit (kg -> g, l -> ml)
// 4. make a key from the name and the dimension, e.g. "chicken|MASS"
//    same key = same product
// 5. add up the quantities with the same key
// 6. change the sums to a nicer unit (1500 g -> 1.5 kg) and sort by name
//
// no Android or Firebase here, so it's tested with normal JUnit tests
public class ShoppingListGenerator {

    private final PortionCalculator portionCalculator;
    private final UnitConverter unitConverter;

    public ShoppingListGenerator() {
        this(new PortionCalculator(), new UnitConverter());
    }

    public ShoppingListGenerator(PortionCalculator portionCalculator, UnitConverter unitConverter) {
        this.portionCalculator = portionCalculator;
        this.unitConverter = unitConverter;
    }

    // plannedMeals - meals chosen by the user, recipesById - all recipes (key = id)
    public GenerationResult generate(List<PlannedMeal> plannedMeals, Map<String, Recipe> recipesById) {
        // LinkedHashMap keeps the order and finds keys fast
        Map<String, ProductSum> sums = new LinkedHashMap<>();
        List<PlannedMeal> skippedMeals = new ArrayList<>();

        if (plannedMeals == null || recipesById == null) {
            return new GenerationResult(new ArrayList<>(), skippedMeals);
        }

        for (PlannedMeal meal : plannedMeals) {
            Recipe recipe = recipesById.get(meal.getRecipeId());
            if (recipe == null || recipe.getDefaultPortions() <= 0 || meal.getPortions() <= 0) {
                skippedMeals.add(meal);
                continue;
            }
            if (recipe.getIngredients() == null) {
                continue;
            }
            for (Ingredient ingredient : recipe.getIngredients()) {
                addIngredient(sums, ingredient, recipe.getDefaultPortions(), meal.getPortions());
            }
        }

        List<ShoppingListItem> items = new ArrayList<>();
        for (ProductSum sum : sums.values()) {
            Quantity readable = unitConverter.normalize(sum.baseQuantity, sum.dimension);
            items.add(new ShoppingListItem(sum.displayName, round(readable.getAmount()),
                    readable.getUnit(), ItemSource.GENERATED));
        }
        Collections.sort(items, new Comparator<ShoppingListItem>() {
            @Override
            public int compare(ShoppingListItem a, ShoppingListItem b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return new GenerationResult(items, skippedMeals);
    }

    // adds one ingredient to the sums, skips wrong ingredients
    private void addIngredient(Map<String, ProductSum> sums, Ingredient ingredient,
                               int defaultPortions, int selectedPortions) {
        if (ingredient == null || ingredient.getUnit() == null
                || ingredient.getQuantity() <= 0 || normalizeName(ingredient.getName()).isEmpty()) {
            return;
        }
        MeasurementUnit unit = ingredient.getUnit();
        double scaled = portionCalculator.calculate(ingredient.getQuantity(), defaultPortions, selectedPortions);
        double baseQuantity = unitConverter.toBaseUnit(scaled, unit);

        String key = normalizeName(ingredient.getName()) + "|" + unit.getDimension();
        ProductSum sum = sums.get(key);
        if (sum == null) {
            sum = new ProductSum(cleanName(ingredient.getName()), unit.getDimension());
            sums.put(key, sum);
        }
        sum.baseQuantity += baseQuantity;
    }

    // name for comparing: no extra spaces, small letters
    // "  Chicken  Breast" and "chicken breast" are the same
    static String normalizeName(String name) {
        return cleanName(name).toLowerCase(Locale.ROOT);
    }

    // name shown to the user: no extra spaces
    static String cleanName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("\\s+", " ");
    }

    // round to 3 decimals to get rid of things like 0.30000000000000004
    static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    // running total for one product
    private static class ProductSum {
        final String displayName;
        final UnitDimension dimension;
        double baseQuantity;

        ProductSum(String displayName, UnitDimension dimension) {
            this.displayName = displayName;
            this.dimension = dimension;
        }
    }
}
