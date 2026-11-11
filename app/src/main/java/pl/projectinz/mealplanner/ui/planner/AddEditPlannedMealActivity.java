package pl.projectinz.mealplanner.ui.planner;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.data.MealPlanRepository;
import pl.projectinz.mealplanner.data.RecipeRepository;
import pl.projectinz.mealplanner.logic.DateUtils;
import pl.projectinz.mealplanner.logic.InputValidator;
import pl.projectinz.mealplanner.logic.RecipeFilter;
import pl.projectinz.mealplanner.model.PlannedMeal;
import pl.projectinz.mealplanner.model.Recipe;
import pl.projectinz.mealplanner.ui.common.EdgeToEdgeHelper;
import pl.projectinz.mealplanner.ui.common.UnsavedChangesGuard;

// add a meal to the planner or edit one
//
// extras:
// EXTRA_MEAL_ID - edit this meal
// EXTRA_DATE - date for a new meal
// EXTRA_RECIPE_ID - recipe for a new meal (from recipe details)
// EXTRA_PORTIONS - portions for a new meal
public class AddEditPlannedMealActivity extends AppCompatActivity {

    public static final String EXTRA_MEAL_ID = "mealId";
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_RECIPE_ID = "recipeId";
    public static final String EXTRA_PORTIONS = "portions";

    private static final String STATE_DATE = "date";
    private static final String STATE_RECIPE_ID = "recipeId";
    private static final String TAG_DATE_PICKER = "mealDatePicker";

    private final MealPlanRepository mealPlanRepository = new MealPlanRepository();
    private final RecipeRepository recipeRepository = new RecipeRepository();
    private final InputValidator validator = new InputValidator();

    private String mealId;
    private PlannedMeal existingMeal;
    private String date;
    private String selectedRecipeId;
    // recipes sorted by name, same order as in the dropdown
    private List<Recipe> recipes = new ArrayList<>();

    private TextInputLayout layoutRecipe;
    private TextInputLayout layoutPortions;
    private TextInputEditText editDate;
    private TextInputEditText editPortions;
    private MaterialAutoCompleteTextView dropdownRecipe;
    private TextView textNoRecipes;
    private MaterialButton buttonSave;
    private UnsavedChangesGuard unsavedChangesGuard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_add_edit_planned_meal);
        EdgeToEdgeHelper.applyPadding(findViewById(R.id.root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        unsavedChangesGuard = new UnsavedChangesGuard(this, this::getFormState);
        toolbar.setNavigationOnClickListener(v -> unsavedChangesGuard.requestClose());

        TextInputLayout layoutDate = findViewById(R.id.layoutDate);
        layoutRecipe = findViewById(R.id.layoutRecipe);
        layoutPortions = findViewById(R.id.layoutPortions);
        editDate = findViewById(R.id.editDate);
        editPortions = findViewById(R.id.editPortions);
        dropdownRecipe = findViewById(R.id.dropdownRecipe);
        textNoRecipes = findViewById(R.id.textNoRecipes);
        buttonSave = findViewById(R.id.buttonSave);

        mealId = getIntent().getStringExtra(EXTRA_MEAL_ID);
        toolbar.setTitle(mealId != null ? R.string.title_edit_planned_meal : R.string.title_add_planned_meal);

        if (savedInstanceState != null) {
            date = savedInstanceState.getString(STATE_DATE);
            selectedRecipeId = savedInstanceState.getString(STATE_RECIPE_ID);
            unsavedChangesGuard.restoreState(savedInstanceState);
        } else {
            String dateExtra = getIntent().getStringExtra(EXTRA_DATE);
            date = DateUtils.isValid(dateExtra) ? dateExtra : DateUtils.today();
            selectedRecipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
            int portions = getIntent().getIntExtra(EXTRA_PORTIONS, 0);
            if (portions > 0) {
                editPortions.setText(String.valueOf(portions));
            }
        }
        showDate();

        editDate.setOnClickListener(v -> openDatePicker());
        layoutDate.setEndIconOnClickListener(v -> openDatePicker());
        reconnectDatePicker();

        dropdownRecipe.setOnItemClickListener((parent, view, position, id) -> {
            Recipe recipe = recipes.get(position);
            selectedRecipeId = recipe.getId();
            layoutRecipe.setError(null);
            // use the default portions of the recipe
            editPortions.setText(String.valueOf(recipe.getDefaultPortions()));
        });
        buttonSave.setOnClickListener(v -> save());

        loadRecipes();
        if (mealId != null) {
            loadMeal(savedInstanceState == null);
        } else if (savedInstanceState == null) {
            unsavedChangesGuard.markClean();
        }
    }

    // all values as one string, to check for unsaved changes
    private String getFormState() {
        return date + "|" + selectedRecipeId + "|" + textOf(editPortions);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_DATE, date);
        outState.putString(STATE_RECIPE_ID, selectedRecipeId);
        unsavedChangesGuard.saveState(outState);
    }

    private void loadRecipes() {
        recipeRepository.getAllRecipesById(new DataCallback<Map<String, Recipe>>() {
            @Override
            public void onSuccess(Map<String, Recipe> result) {
                recipes = new ArrayList<>(result.values());
                new RecipeFilter().sortByName(recipes);

                String[] names = new String[recipes.size()];
                for (int i = 0; i < recipes.size(); i++) {
                    names[i] = recipes.get(i).getName();
                }
                dropdownRecipe.setSimpleItems(names);
                textNoRecipes.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);
                showSelectedRecipe();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AddEditPlannedMealActivity.this, R.string.error_generic, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadMeal(final boolean fillFields) {
        buttonSave.setEnabled(false);
        mealPlanRepository.getMeal(mealId, new DataCallback<PlannedMeal>() {
            @Override
            public void onSuccess(PlannedMeal meal) {
                if (meal == null) {
                    finish();
                    return;
                }
                existingMeal = meal;
                buttonSave.setEnabled(true);
                if (fillFields) {
                    date = meal.getDate();
                    selectedRecipeId = meal.getRecipeId();
                    editPortions.setText(String.valueOf(meal.getPortions()));
                    showDate();
                    showSelectedRecipe();
                    unsavedChangesGuard.markClean();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AddEditPlannedMealActivity.this, R.string.error_generic, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    // show the recipe name in the dropdown (when we have both the list and the id)
    private void showSelectedRecipe() {
        Recipe recipe = findRecipe(selectedRecipeId);
        if (recipe != null) {
            dropdownRecipe.setText(recipe.getName(), false);
            if (editPortions.getText() == null || editPortions.getText().length() == 0) {
                editPortions.setText(String.valueOf(recipe.getDefaultPortions()));
            }
        }
    }

    private Recipe findRecipe(String recipeId) {
        if (recipeId == null) {
            return null;
        }
        for (Recipe recipe : recipes) {
            if (recipeId.equals(recipe.getId())) {
                return recipe;
            }
        }
        return null;
    }

    private void showDate() {
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        editDate.setText(DateUtils.formatForDisplay(date, "EEEE, d MMMM yyyy", locale));
    }

    private void openDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.planner_pick_date)
                .setSelection(DateUtils.toUtcMillis(date))
                .build();
        picker.addOnPositiveButtonClickListener(this::onDatePicked);
        picker.show(getSupportFragmentManager(), TAG_DATE_PICKER);
    }

    // after rotation the date picker is created again by the system - connect it again
    @SuppressWarnings("unchecked")
    private void reconnectDatePicker() {
        MaterialDatePicker<Long> picker =
                (MaterialDatePicker<Long>) getSupportFragmentManager().findFragmentByTag(TAG_DATE_PICKER);
        if (picker != null) {
            picker.addOnPositiveButtonClickListener(this::onDatePicked);
        }
    }

    // the picker gives UTC millis, convert in UTC so the day doesn't change
    private void onDatePicked(Long utcMillis) {
        date = DateUtils.fromUtcMillis(utcMillis);
        showDate();
    }

    private void save() {
        boolean valid = true;
        layoutRecipe.setError(null);
        layoutPortions.setError(null);

        Recipe recipe = findRecipe(selectedRecipeId);
        if (recipe == null) {
            layoutRecipe.setError(getString(R.string.error_recipe_required));
            valid = false;
        }
        Integer portions = validator.parsePortions(textOf(editPortions));
        if (portions == null) {
            layoutPortions.setError(getString(R.string.error_portions_invalid, InputValidator.MAX_PORTIONS));
            valid = false;
        }
        if (!valid) {
            return;
        }

        PlannedMeal meal = existingMeal != null ? existingMeal : new PlannedMeal();
        meal.setRecipeId(recipe.getId());
        meal.setRecipeName(recipe.getName());
        meal.setDate(date);
        meal.setPortions(portions);

        DataCallback<Void> callback = new DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getApplicationContext(), R.string.error_save_failed, Toast.LENGTH_LONG).show();
            }
        };
        if (existingMeal != null) {
            mealPlanRepository.updateMeal(meal, callback);
        } else {
            mealPlanRepository.addMeal(meal, callback);
        }
        finish();
    }

    private static String textOf(TextView view) {
        return view.getText() != null ? view.getText().toString() : "";
    }
}
