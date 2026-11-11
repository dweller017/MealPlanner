package pl.projectinz.mealplanner.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.projectinz.mealplanner.model.Ingredient;
import pl.projectinz.mealplanner.model.ItemSource;
import pl.projectinz.mealplanner.model.MeasurementUnit;
import pl.projectinz.mealplanner.model.PlannedMeal;
import pl.projectinz.mealplanner.model.Recipe;
import pl.projectinz.mealplanner.model.ShoppingListItem;

public class ShoppingListGeneratorTest {

    private static final double DELTA = 0.0001;

    private ShoppingListGenerator generator;
    private Map<String, Recipe> recipes;

    @Before
    public void setUp() {
        generator = new ShoppingListGenerator();
        recipes = new HashMap<>();
    }

    // helpers

    private Recipe addRecipe(String id, int defaultPortions, Ingredient... ingredients) {
        Recipe recipe = new Recipe("Recipe " + id, "", defaultPortions,
                new ArrayList<>(Arrays.asList(ingredients)));
        recipe.setId(id);
        recipes.put(id, recipe);
        return recipe;
    }

    private static Ingredient ing(String name, double quantity, MeasurementUnit unit) {
        return new Ingredient(name, quantity, unit);
    }

    private static PlannedMeal meal(String recipeId, int portions) {
        return new PlannedMeal(recipeId, "Recipe " + recipeId, "2026-11-15", portions);
    }

    private static ShoppingListItem find(List<ShoppingListItem> items, String name) {
        for (ShoppingListItem item : items) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    // example from the project description

    @Test
    public void combinesTwoRecipesFromSpecification() {
        addRecipe("A", 2, ing("Chicken", 200, MeasurementUnit.G), ing("Rice", 100, MeasurementUnit.G));
        addRecipe("B", 2, ing("Chicken", 300, MeasurementUnit.G), ing("Onion", 1, MeasurementUnit.PCS));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 2), meal("B", 2)), recipes).getItems();

        assertEquals(3, items.size());
        assertEquals(500.0, find(items, "Chicken").getQuantity(), DELTA);
        assertEquals(MeasurementUnit.G, find(items, "Chicken").getUnit());
        assertEquals(100.0, find(items, "Rice").getQuantity(), DELTA);
        assertEquals(1.0, find(items, "Onion").getQuantity(), DELTA);
        assertEquals(MeasurementUnit.PCS, find(items, "Onion").getUnit());
    }

    // portions

    @Test
    public void recalculatesPortions() {
        addRecipe("A", 2, ing("Chicken", 200, MeasurementUnit.G), ing("Rice", 100, MeasurementUnit.G));

        List<ShoppingListItem> items = generator.generate(
                Collections.singletonList(meal("A", 4)), recipes).getItems();

        assertEquals(400.0, find(items, "Chicken").getQuantity(), DELTA);
        assertEquals(200.0, find(items, "Rice").getQuantity(), DELTA);
    }

    @Test
    public void combinesRecipesWithDifferentPortionCounts() {
        // A: 2 portions, 200 g chicken -> planned 3 portions = 300 g
        // B: 4 portions, 400 g chicken -> planned 1 portion  = 100 g
        addRecipe("A", 2, ing("Chicken", 200, MeasurementUnit.G));
        addRecipe("B", 4, ing("Chicken", 400, MeasurementUnit.G), ing("Milk", 1, MeasurementUnit.L));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 3), meal("B", 1)), recipes).getItems();

        assertEquals(2, items.size());
        assertEquals(400.0, find(items, "Chicken").getQuantity(), DELTA);
        // 1 l * 1 / 4 = 0.25 l = 250 ml
        assertEquals(250.0, find(items, "Milk").getQuantity(), DELTA);
        assertEquals(MeasurementUnit.ML, find(items, "Milk").getUnit());
    }

    @Test
    public void sameRecipePlannedTwiceIsCountedTwice() {
        addRecipe("A", 2, ing("Eggs", 3, MeasurementUnit.PCS));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 2), meal("A", 4)), recipes).getItems();

        assertEquals(1, items.size());
        assertEquals(9.0, items.get(0).getQuantity(), DELTA);
    }

    // same ingredients

    @Test
    public void sumsIdenticalIngredients() {
        addRecipe("A", 1, ing("Chicken", 200, MeasurementUnit.G));
        addRecipe("B", 1, ing("Chicken", 300, MeasurementUnit.G));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 1), meal("B", 1)), recipes).getItems();

        assertEquals(1, items.size());
        assertEquals(500.0, items.get(0).getQuantity(), DELTA);
    }

    @Test
    public void namesAreComparedIgnoringCaseAndSpaces() {
        addRecipe("A", 1, ing("Chicken breast", 200, MeasurementUnit.G));
        addRecipe("B", 1, ing("  chicken   BREAST ", 300, MeasurementUnit.G));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 1), meal("B", 1)), recipes).getItems();

        assertEquals(1, items.size());
        assertEquals("Chicken breast", items.get(0).getName());
        assertEquals(500.0, items.get(0).getQuantity(), DELTA);
    }

    @Test
    public void differentIngredientsStaySeparate() {
        addRecipe("A", 1, ing("Tomato", 2, MeasurementUnit.PCS), ing("Cucumber", 1, MeasurementUnit.PCS),
                ing("Salt", 5, MeasurementUnit.G));

        List<ShoppingListItem> items = generator.generate(
                Collections.singletonList(meal("A", 1)), recipes).getItems();

        assertEquals(3, items.size());
        assertNotNull(find(items, "Tomato"));
        assertNotNull(find(items, "Cucumber"));
        assertNotNull(find(items, "Salt"));
    }

    // units

    @Test
    public void sumsGramsAndKilograms() {
        // 500 g + 1 kg = 1.5 kg
        addRecipe("A", 1, ing("Flour", 500, MeasurementUnit.G));
        addRecipe("B", 1, ing("Flour", 1, MeasurementUnit.KG));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 1), meal("B", 1)), recipes).getItems();

        assertEquals(1, items.size());
        assertEquals(1.5, items.get(0).getQuantity(), DELTA);
        assertEquals(MeasurementUnit.KG, items.get(0).getUnit());
    }

    @Test
    public void sumsMillilitresAndLitres() {
        // 750 ml + 0.5 l = 1.25 l
        addRecipe("A", 1, ing("Milk", 750, MeasurementUnit.ML));
        addRecipe("B", 1, ing("Milk", 0.5, MeasurementUnit.L));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 1), meal("B", 1)), recipes).getItems();

        assertEquals(1, items.size());
        assertEquals(1.25, items.get(0).getQuantity(), DELTA);
        assertEquals(MeasurementUnit.L, items.get(0).getUnit());
    }

    @Test
    public void incompatibleUnitsAreNotCombined() {
        // grams and pieces cannot be converted -> two separate items
        addRecipe("A", 1, ing("Chicken", 200, MeasurementUnit.G));
        addRecipe("B", 1, ing("Chicken", 1, MeasurementUnit.PCS));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 1), meal("B", 1)), recipes).getItems();

        assertEquals(2, items.size());
    }

    // edge cases

    @Test
    public void emptyMealListGivesEmptyShoppingList() {
        GenerationResult result = generator.generate(new ArrayList<PlannedMeal>(), recipes);
        assertTrue(result.getItems().isEmpty());
        assertTrue(result.getSkippedMeals().isEmpty());
    }

    @Test
    public void nullInputGivesEmptyShoppingList() {
        GenerationResult result = generator.generate(null, null);
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    public void recipeWithoutIngredientsGivesEmptyShoppingList() {
        addRecipe("A", 2);
        GenerationResult result = generator.generate(Collections.singletonList(meal("A", 2)), recipes);
        assertTrue(result.getItems().isEmpty());
        assertTrue(result.getSkippedMeals().isEmpty());
    }

    @Test
    public void singlePlannedMeal() {
        addRecipe("A", 1, ing("Pasta", 100, MeasurementUnit.G));
        List<ShoppingListItem> items = generator.generate(
                Collections.singletonList(meal("A", 1)), recipes).getItems();
        assertEquals(1, items.size());
        assertEquals(100.0, items.get(0).getQuantity(), DELTA);
    }

    @Test
    public void severalPlannedMeals() {
        addRecipe("A", 1, ing("Pasta", 100, MeasurementUnit.G));
        addRecipe("B", 1, ing("Pasta", 150, MeasurementUnit.G), ing("Cheese", 50, MeasurementUnit.G));
        addRecipe("C", 2, ing("Pasta", 200, MeasurementUnit.G), ing("Tomato", 2, MeasurementUnit.PCS));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 2), meal("B", 1), meal("C", 1)), recipes).getItems();

        // pasta: 200 + 150 + 100 = 450 g
        assertEquals(3, items.size());
        assertEquals(450.0, find(items, "Pasta").getQuantity(), DELTA);
        assertEquals(50.0, find(items, "Cheese").getQuantity(), DELTA);
        assertEquals(1.0, find(items, "Tomato").getQuantity(), DELTA);
    }

    @Test
    public void decimalQuantitiesAreSummedAndRounded() {
        // 0.1 kg + 0.2 kg = 0.3 kg -> 300 g (no 300.00000000000006)
        addRecipe("A", 1, ing("Butter", 0.1, MeasurementUnit.KG));
        addRecipe("B", 1, ing("Butter", 0.2, MeasurementUnit.KG));

        List<ShoppingListItem> items = generator.generate(
                Arrays.asList(meal("A", 1), meal("B", 1)), recipes).getItems();

        assertEquals(300.0, items.get(0).getQuantity(), 0.0);
        assertEquals(MeasurementUnit.G, items.get(0).getUnit());
    }

    @Test
    public void mealWithDeletedRecipeIsSkipped() {
        addRecipe("A", 1, ing("Rice", 100, MeasurementUnit.G));
        PlannedMeal missing = meal("DELETED", 2);

        GenerationResult result = generator.generate(Arrays.asList(meal("A", 1), missing), recipes);

        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getSkippedMeals().size());
        assertEquals("DELETED", result.getSkippedMeals().get(0).getRecipeId());
    }

    @Test
    public void mealWithInvalidPortionsIsSkipped() {
        addRecipe("A", 1, ing("Rice", 100, MeasurementUnit.G));
        GenerationResult result = generator.generate(Collections.singletonList(meal("A", 0)), recipes);
        assertTrue(result.getItems().isEmpty());
        assertEquals(1, result.getSkippedMeals().size());
    }

    @Test
    public void invalidIngredientsAreIgnored() {
        addRecipe("A", 1,
                ing("", 100, MeasurementUnit.G),
                ing("Sugar", 0, MeasurementUnit.G),
                ing("Salt", 5, null),
                ing("Rice", 100, MeasurementUnit.G));

        List<ShoppingListItem> items = generator.generate(
                Collections.singletonList(meal("A", 1)), recipes).getItems();

        assertEquals(1, items.size());
        assertEquals("Rice", items.get(0).getName());
    }

    @Test
    public void generatedItemsAreMarkedAndSortedByName() {
        addRecipe("A", 1, ing("onion", 1, MeasurementUnit.PCS), ing("Apple", 2, MeasurementUnit.PCS),
                ing("butter", 50, MeasurementUnit.G));

        List<ShoppingListItem> items = generator.generate(
                Collections.singletonList(meal("A", 1)), recipes).getItems();

        assertEquals("Apple", items.get(0).getName());
        assertEquals("butter", items.get(1).getName());
        assertEquals("onion", items.get(2).getName());
        for (ShoppingListItem item : items) {
            assertEquals(ItemSource.GENERATED, item.getSource());
            assertEquals(false, item.isPurchased());
            assertNull(item.getId());
        }
    }
}
