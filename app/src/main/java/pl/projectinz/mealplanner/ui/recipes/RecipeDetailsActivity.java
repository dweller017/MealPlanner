package pl.projectinz.mealplanner.ui.recipes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.data.MealPlanRepository;
import pl.projectinz.mealplanner.data.RecipeRepository;
import pl.projectinz.mealplanner.logic.PortionCalculator;
import pl.projectinz.mealplanner.model.Ingredient;
import pl.projectinz.mealplanner.model.Recipe;
import pl.projectinz.mealplanner.ui.common.EdgeToEdgeHelper;
import pl.projectinz.mealplanner.ui.common.UnitLabels;
import pl.projectinz.mealplanner.ui.planner.AddEditPlannedMealActivity;

// shows one recipe. the user can change the portions and the quantities
// are recalculated with PortionCalculator. toolbar: favourite, edit, delete
public class RecipeDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipeId";
    // optional: portions to show at the start (e.g. from a planned meal)
    public static final String EXTRA_PORTIONS = "portions";
    private static final String STATE_PORTIONS = "selectedPortions";
    private static final int MAX_PORTIONS = 99;

    private final RecipeRepository recipeRepository = new RecipeRepository();
    private final MealPlanRepository mealPlanRepository = new MealPlanRepository();
    private final PortionCalculator portionCalculator = new PortionCalculator();

    private String recipeId;
    private Recipe recipe;
    // portions chosen by the user, 0 = use the recipe default
    private int selectedPortions;
    private ListenerRegistration recipeListener;

    private MaterialToolbar toolbar;
    private TextView textName;
    private TextView textCategory;
    private TextView textDescription;
    private TextView textPortions;
    private TextView textDefaultPortions;
    private LinearLayout containerIngredients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_recipe_details);
        EdgeToEdgeHelper.applyPadding(findViewById(R.id.root));

        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (recipeId == null) {
            finish();
            return;
        }
        if (savedInstanceState != null) {
            selectedPortions = savedInstanceState.getInt(STATE_PORTIONS);
        } else {
            selectedPortions = getIntent().getIntExtra(EXTRA_PORTIONS, 0);
        }

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(this::onMenuItemClicked);

        textName = findViewById(R.id.textName);
        textCategory = findViewById(R.id.textCategory);
        textDescription = findViewById(R.id.textDescription);
        textPortions = findViewById(R.id.textPortions);
        textDefaultPortions = findViewById(R.id.textDefaultPortions);
        containerIngredients = findViewById(R.id.containerIngredients);

        findViewById(R.id.buttonMinus).setOnClickListener(v -> changePortions(-1));
        findViewById(R.id.buttonPlus).setOnClickListener(v -> changePortions(1));
        findViewById(R.id.buttonPlanMeal).setOnClickListener(v -> planMeal());
    }

    // opens the planner form with this recipe and portions
    private void planMeal() {
        if (recipe == null) {
            return;
        }
        Intent intent = new Intent(this, AddEditPlannedMealActivity.class);
        intent.putExtra(AddEditPlannedMealActivity.EXTRA_RECIPE_ID, recipeId);
        intent.putExtra(AddEditPlannedMealActivity.EXTRA_PORTIONS, selectedPortions);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // listening keeps the screen updated, e.g. after editing
        recipeListener = recipeRepository.listenToRecipe(recipeId, new DataCallback<Recipe>() {
            @Override
            public void onSuccess(Recipe result) {
                if (result == null) {
                    // the recipe was deleted
                    finish();
                    return;
                }
                recipe = result;
                if (selectedPortions <= 0) {
                    selectedPortions = recipe.getDefaultPortions();
                }
                showRecipe();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(RecipeDetailsActivity.this, R.string.error_generic, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recipeListener != null) {
            recipeListener.remove();
            recipeListener = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PORTIONS, selectedPortions);
    }

    private void showRecipe() {
        textName.setText(recipe.getName());

        boolean hasCategory = recipe.getCategory() != null && !recipe.getCategory().isEmpty();
        textCategory.setVisibility(hasCategory ? View.VISIBLE : View.GONE);
        textCategory.setText(recipe.getCategory());

        boolean hasDescription = recipe.getDescription() != null && !recipe.getDescription().isEmpty();
        textDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        textDescription.setText(recipe.getDescription());

        textDefaultPortions.setText(getResources().getQuantityString(R.plurals.recipe_is_for_portions,
                recipe.getDefaultPortions(), recipe.getDefaultPortions()));

        MenuItem favouriteItem = toolbar.getMenu().findItem(R.id.action_favourite);
        favouriteItem.setIcon(recipe.isFavourite() ? R.drawable.ic_star : R.drawable.ic_star_border);

        showIngredients();
    }

    // shows the ingredients for the chosen portions
    private void showIngredients() {
        textPortions.setText(String.valueOf(selectedPortions));
        containerIngredients.removeAllViews();
        if (recipe.getIngredients() == null || recipe.getDefaultPortions() <= 0) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Ingredient ingredient : recipe.getIngredients()) {
            View line = inflater.inflate(R.layout.item_ingredient_line, containerIngredients, false);
            double quantity = portionCalculator.calculate(ingredient.getQuantity(),
                    recipe.getDefaultPortions(), selectedPortions);
            ((TextView) line.findViewById(R.id.textIngredientName)).setText(ingredient.getName());
            ((TextView) line.findViewById(R.id.textIngredientQuantity))
                    .setText(UnitLabels.formatQuantity(this, quantity, ingredient.getUnit()));
            containerIngredients.addView(line);
        }
    }

    private void changePortions(int difference) {
        if (recipe == null) {
            return;
        }
        int newValue = selectedPortions + difference;
        if (newValue >= 1 && newValue <= MAX_PORTIONS) {
            selectedPortions = newValue;
            showIngredients();
        }
    }

    private boolean onMenuItemClicked(MenuItem item) {
        if (recipe == null) {
            return false;
        }
        int id = item.getItemId();
        if (id == R.id.action_favourite) {
            recipeRepository.setFavourite(recipeId, !recipe.isFavourite(), ignoreSuccess());
            return true;
        } else if (id == R.id.action_edit) {
            Intent intent = new Intent(this, AddEditRecipeActivity.class);
            intent.putExtra(AddEditRecipeActivity.EXTRA_RECIPE_ID, recipeId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return false;
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_recipe_title)
                .setMessage(getString(R.string.delete_recipe_message, recipe.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    recipeRepository.deleteRecipe(recipeId, ignoreSuccess());
                    // delete the planned meals of this recipe too
                    mealPlanRepository.deleteMealsForRecipe(recipeId);
                    finish();
                })
                .show();
    }

    // callback that only shows errors (the listener shows the changes)
    private DataCallback<Void> ignoreSuccess() {
        return new DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getApplicationContext(), R.string.error_save_failed, Toast.LENGTH_LONG).show();
            }
        };
    }
}
