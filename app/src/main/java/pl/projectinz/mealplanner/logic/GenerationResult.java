package pl.projectinz.mealplanner.logic;

import java.util.List;

import pl.projectinz.mealplanner.model.PlannedMeal;
import pl.projectinz.mealplanner.model.ShoppingListItem;

// result of generating: the products + meals that were skipped
// (for example when the recipe was deleted)
public class GenerationResult {

    private final List<ShoppingListItem> items;
    private final List<PlannedMeal> skippedMeals;

    public GenerationResult(List<ShoppingListItem> items, List<PlannedMeal> skippedMeals) {
        this.items = items;
        this.skippedMeals = skippedMeals;
    }

    public List<ShoppingListItem> getItems() {
        return items;
    }

    public List<PlannedMeal> getSkippedMeals() {
        return skippedMeals;
    }
}
