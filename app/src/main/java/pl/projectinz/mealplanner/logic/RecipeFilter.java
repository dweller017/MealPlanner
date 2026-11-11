package pl.projectinz.mealplanner.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import pl.projectinz.mealplanner.model.Ingredient;
import pl.projectinz.mealplanner.model.Recipe;

// search and favourites filter for recipes
//
// Firestore can't search for a part of a text, so we load the recipes
// and filter them on the phone. The search looks in the name,
// the category and the ingredient names (big/small letters don't matter)
public class RecipeFilter {

    public List<Recipe> filter(List<Recipe> recipes, String query, boolean favouritesOnly) {
        List<Recipe> result = new ArrayList<>();
        if (recipes == null) {
            return result;
        }
        String normalizedQuery = normalize(query);

        for (Recipe recipe : recipes) {
            if (favouritesOnly && !recipe.isFavourite()) {
                continue;
            }
            if (normalizedQuery.isEmpty() || matches(recipe, normalizedQuery)) {
                result.add(recipe);
            }
        }
        sortByName(result);
        return result;
    }

    // sorts by name, big/small letters don't matter
    public void sortByName(List<Recipe> recipes) {
        Collections.sort(recipes, new Comparator<Recipe>() {
            @Override
            public int compare(Recipe a, Recipe b) {
                return normalize(a.getName()).compareTo(normalize(b.getName()));
            }
        });
    }

    private boolean matches(Recipe recipe, String query) {
        if (normalize(recipe.getName()).contains(query)) {
            return true;
        }
        if (normalize(recipe.getCategory()).contains(query)) {
            return true;
        }
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (normalize(ingredient.getName()).contains(query)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
