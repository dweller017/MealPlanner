package pl.projectinz.mealplanner.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pl.projectinz.mealplanner.model.Ingredient;
import pl.projectinz.mealplanner.model.MeasurementUnit;
import pl.projectinz.mealplanner.model.Recipe;

public class RecipeFilterTest {

    private final RecipeFilter filter = new RecipeFilter();
    private List<Recipe> recipes;

    private static Recipe recipe(String name, String category, boolean favourite, String... ingredientNames) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (String ingredientName : ingredientNames) {
            ingredients.add(new Ingredient(ingredientName, 1, MeasurementUnit.PCS));
        }
        Recipe recipe = new Recipe(name, "", 2, ingredients);
        recipe.setCategory(category);
        recipe.setFavourite(favourite);
        return recipe;
    }

    @Before
    public void setUp() {
        recipes = Arrays.asList(
                recipe("Chicken curry", "Dinner", true, "Chicken", "Rice"),
                recipe("pancakes", "Breakfast", false, "Flour", "Milk", "Eggs"),
                recipe("Tomato soup", null, false, "Tomato", "Onion"));
    }

    @Test
    public void emptyQueryReturnsAllSortedByName() {
        List<Recipe> result = filter.filter(recipes, "", false);
        assertEquals(3, result.size());
        assertEquals("Chicken curry", result.get(0).getName());
        assertEquals("pancakes", result.get(1).getName());
        assertEquals("Tomato soup", result.get(2).getName());
    }

    @Test
    public void searchesNameIgnoringCase() {
        List<Recipe> result = filter.filter(recipes, "CURRY", false);
        assertEquals(1, result.size());
        assertEquals("Chicken curry", result.get(0).getName());
    }

    @Test
    public void searchesCategory() {
        assertEquals(1, filter.filter(recipes, "breakfast", false).size());
    }

    @Test
    public void searchesIngredientNames() {
        List<Recipe> result = filter.filter(recipes, "onion", false);
        assertEquals(1, result.size());
        assertEquals("Tomato soup", result.get(0).getName());
    }

    @Test
    public void favouritesOnly() {
        List<Recipe> result = filter.filter(recipes, null, true);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isFavourite());
    }

    @Test
    public void favouritesAndQueryTogether() {
        assertEquals(0, filter.filter(recipes, "pancakes", true).size());
    }

    @Test
    public void noMatchGivesEmptyList() {
        assertTrue(filter.filter(recipes, "pizza", false).isEmpty());
        assertTrue(filter.filter(null, "pizza", false).isEmpty());
    }
}
