package pl.projectinz.mealplanner.data;

import java.util.List;
import java.util.Map;

import pl.projectinz.mealplanner.logic.GenerationResult;
import pl.projectinz.mealplanner.logic.ShoppingListGenerator;
import pl.projectinz.mealplanner.logic.ShoppingListOrganizer;
import pl.projectinz.mealplanner.model.PlannedMeal;
import pl.projectinz.mealplanner.model.Recipe;
import pl.projectinz.mealplanner.model.ShoppingListItem;

// connects the repositories with ShoppingListGenerator
//
// steps:
// 1. read planned meals in the date range
// 2. read the recipes
// 3. calculate the list (ShoppingListGenerator)
// 4. keep categories and "purchased" from the old list
// 5. replace the old generated products
//
// the date range is saved, so refresh() can calculate the list again
// when the plan changes (it only writes to the database if something changed)
public class ShoppingListGenerationService {

    // info shown to the user after generating
    public static class Summary {
        private final String fromDate;
        private final String toDate;
        private final int mealCount;
        private final int productCount;
        private final int skippedMealCount;
        private final boolean changed;

        public Summary(String fromDate, String toDate, int mealCount, int productCount,
                       int skippedMealCount, boolean changed) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.mealCount = mealCount;
            this.productCount = productCount;
            this.skippedMealCount = skippedMealCount;
            this.changed = changed;
        }

        public String getFromDate() {
            return fromDate;
        }

        public String getToDate() {
            return toDate;
        }

        public int getMealCount() {
            return mealCount;
        }

        public int getProductCount() {
            return productCount;
        }

        public int getSkippedMealCount() {
            return skippedMealCount;
        }

        // true if the list in the database was changed
        public boolean isChanged() {
            return changed;
        }
    }

    private final MealPlanRepository mealPlanRepository = new MealPlanRepository();
    private final RecipeRepository recipeRepository = new RecipeRepository();
    private final ShoppingListRepository shoppingListRepository = new ShoppingListRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ShoppingListGenerator generator = new ShoppingListGenerator();
    private final ShoppingListOrganizer organizer = new ShoppingListOrganizer();

    // generates the list for the dates and saves the date range
    // if there are no meals in the range nothing changes
    public void generate(final String fromDate, final String toDate, final DataCallback<Summary> callback) {
        mealPlanRepository.getMealsBetween(fromDate, toDate, new DataCallback<List<PlannedMeal>>() {
            @Override
            public void onSuccess(List<PlannedMeal> meals) {
                if (meals.isEmpty()) {
                    callback.onSuccess(new Summary(fromDate, toDate, 0, 0, 0, false));
                    return;
                }
                userRepository.saveShoppingRange(fromDate, toDate);
                loadRecipesAndGenerate(fromDate, toDate, meals, true, callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // calculates the list again for the saved date range
    // gives null if the list was never generated
    public void refresh(final DataCallback<Summary> callback) {
        userRepository.getShoppingRange(new DataCallback<String[]>() {
            @Override
            public void onSuccess(String[] range) {
                if (range == null) {
                    callback.onSuccess(null);
                    return;
                }
                final String fromDate = range[0];
                final String toDate = range[1];
                mealPlanRepository.getMealsBetween(fromDate, toDate, new DataCallback<List<PlannedMeal>>() {
                    @Override
                    public void onSuccess(List<PlannedMeal> meals) {
                        // also when there are no meals - then the generated products get removed
                        loadRecipesAndGenerate(fromDate, toDate, meals, false, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void loadRecipesAndGenerate(final String fromDate, final String toDate, final List<PlannedMeal> meals,
                                        final boolean alwaysSave, final DataCallback<Summary> callback) {
        recipeRepository.getAllRecipesById(new DataCallback<Map<String, Recipe>>() {
            @Override
            public void onSuccess(Map<String, Recipe> recipesById) {
                GenerationResult result = generator.generate(meals, recipesById);
                saveResult(fromDate, toDate, meals.size(), result, alwaysSave, callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void saveResult(final String fromDate, final String toDate, final int mealCount,
                            final GenerationResult result, final boolean alwaysSave,
                            final DataCallback<Summary> callback) {
        shoppingListRepository.getAllItems(new DataCallback<List<ShoppingListItem>>() {
            @Override
            public void onSuccess(List<ShoppingListItem> currentItems) {
                final List<ShoppingListItem> newItems = result.getItems();
                final int skipped = result.getSkippedMeals().size();

                if (!alwaysSave && organizer.hasSameGeneratedContent(currentItems, newItems)) {
                    // nothing changed, so no need to save
                    callback.onSuccess(new Summary(fromDate, toDate, mealCount, newItems.size(), skipped, false));
                    return;
                }
                organizer.copyKnownCategories(newItems, currentItems);
                organizer.copyPurchasedState(newItems, currentItems);
                shoppingListRepository.replaceGeneratedItems(currentItems, newItems, new DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        callback.onSuccess(new Summary(fromDate, toDate, mealCount, newItems.size(), skipped, true));
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
}
