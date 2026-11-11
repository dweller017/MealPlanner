package pl.projectinz.mealplanner.ui.recipes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.data.MealPlanRepository;
import pl.projectinz.mealplanner.data.RecipeRepository;
import pl.projectinz.mealplanner.logic.InputValidator;
import pl.projectinz.mealplanner.logic.QuantityFormatter;
import pl.projectinz.mealplanner.model.Ingredient;
import pl.projectinz.mealplanner.model.MeasurementUnit;
import pl.projectinz.mealplanner.model.Recipe;
import pl.projectinz.mealplanner.ui.common.EdgeToEdgeHelper;
import pl.projectinz.mealplanner.ui.common.UnitLabels;
import pl.projectinz.mealplanner.ui.common.UnsavedChangesGuard;

// add a new recipe or edit one (when EXTRA_RECIPE_ID is given)
// ingredients are rows that can be added and removed,
// each row is item_ingredient_input.xml + IngredientRow
public class AddEditRecipeActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipeId";

    // keys for saving the ingredient rows on rotation
    private static final String STATE_NAMES = "ingredientNames";
    private static final String STATE_QUANTITIES = "ingredientQuantities";
    private static final String STATE_UNITS = "ingredientUnits";

    private final RecipeRepository recipeRepository = new RecipeRepository();
    private final InputValidator validator = new InputValidator();

    // the recipe we edit, null for a new one
    private Recipe existingRecipe;
    private String recipeId;

    private TextInputLayout layoutName;
    private TextInputLayout layoutPortions;
    private TextInputEditText editName;
    private TextInputEditText editPortions;
    private TextInputEditText editDescription;
    private MaterialAutoCompleteTextView editCategory;
    private TextView textIngredientsError;
    private LinearLayout containerIngredients;
    private MaterialButton buttonSave;

    private final List<IngredientRow> rows = new ArrayList<>();
    private String[] unitLabels;
    private UnsavedChangesGuard unsavedChangesGuard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_add_edit_recipe);
        EdgeToEdgeHelper.applyPadding(findViewById(R.id.root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        // back asks before losing changes
        unsavedChangesGuard = new UnsavedChangesGuard(this, this::getFormState);
        toolbar.setNavigationOnClickListener(v -> unsavedChangesGuard.requestClose());

        layoutName = findViewById(R.id.layoutName);
        layoutPortions = findViewById(R.id.layoutPortions);
        editName = findViewById(R.id.editName);
        editPortions = findViewById(R.id.editPortions);
        editDescription = findViewById(R.id.editDescription);
        editCategory = findViewById(R.id.editCategory);
        textIngredientsError = findViewById(R.id.textIngredientsError);
        containerIngredients = findViewById(R.id.containerIngredients);
        buttonSave = findViewById(R.id.buttonSave);

        unitLabels = UnitLabels.getAllLabels(this);
        editCategory.setSimpleItems(getResources().getStringArray(R.array.recipe_categories));

        findViewById(R.id.buttonAddIngredient).setOnClickListener(v -> addIngredientRow("", "", -1));
        buttonSave.setOnClickListener(v -> saveRecipe());

        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        boolean editMode = recipeId != null;
        toolbar.setTitle(editMode ? R.string.title_edit_recipe : R.string.title_add_recipe);

        if (savedInstanceState != null) {
            // screen was recreated (rotation) - get the rows back
            restoreRows(savedInstanceState);
            unsavedChangesGuard.restoreState(savedInstanceState);
        } else if (!editMode) {
            // new recipe: one empty row and 2 portions
            editPortions.setText("2");
            addIngredientRow("", "", -1);
            unsavedChangesGuard.markClean();
        }

        if (editMode) {
            loadRecipe(savedInstanceState == null);
        }
    }

    // loads the recipe to edit, fills the form only the first time
    private void loadRecipe(final boolean fillFields) {
        buttonSave.setEnabled(false);
        recipeRepository.getRecipe(recipeId, new DataCallback<Recipe>() {
            @Override
            public void onSuccess(Recipe recipe) {
                if (recipe == null) {
                    finish();
                    return;
                }
                existingRecipe = recipe;
                buttonSave.setEnabled(true);
                if (fillFields) {
                    fillForm(recipe);
                    unsavedChangesGuard.markClean();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AddEditRecipeActivity.this, R.string.error_generic, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void fillForm(Recipe recipe) {
        editName.setText(recipe.getName());
        editPortions.setText(String.valueOf(recipe.getDefaultPortions()));
        editDescription.setText(recipe.getDescription());
        editCategory.setText(recipe.getCategory(), false);

        QuantityFormatter formatter = new QuantityFormatter();
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                int unitIndex = ingredient.getUnit() != null ? ingredient.getUnit().ordinal() : -1;
                addIngredientRow(ingredient.getName(),
                        formatter.format(ingredient.getQuantity(), locale), unitIndex);
            }
        }
    }

    // adds a row, unitIndex = position in MeasurementUnit.values(), -1 = nothing chosen
    private void addIngredientRow(String name, String quantity, int unitIndex) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_ingredient_input, containerIngredients, false);
        final IngredientRow row = new IngredientRow(view);

        row.editName.setText(name);
        row.editQuantity.setText(quantity);
        row.dropdownUnit.setSimpleItems(unitLabels);
        if (unitIndex >= 0) {
            row.unitIndex = unitIndex;
            row.dropdownUnit.setText(unitLabels[unitIndex], false);
        }
        row.dropdownUnit.setOnItemClickListener((parent, itemView, position, id) -> {
            row.unitIndex = position;
            row.layoutUnit.setError(null);
        });
        row.buttonRemove.setOnClickListener(v -> {
            containerIngredients.removeView(row.root);
            rows.remove(row);
        });

        rows.add(row);
        containerIngredients.addView(view);
        textIngredientsError.setVisibility(View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> quantities = new ArrayList<>();
        int[] units = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            names.add(textOf(rows.get(i).editName));
            quantities.add(textOf(rows.get(i).editQuantity));
            units[i] = rows.get(i).unitIndex;
        }
        outState.putStringArrayList(STATE_NAMES, names);
        outState.putStringArrayList(STATE_QUANTITIES, quantities);
        outState.putIntArray(STATE_UNITS, units);
        unsavedChangesGuard.saveState(outState);
    }

    // all values as one string, to check for unsaved changes
    private String getFormState() {
        StringBuilder state = new StringBuilder();
        state.append(textOf(editName)).append('|')
                .append(textOf(editPortions)).append('|')
                .append(textOf(editCategory)).append('|')
                .append(textOf(editDescription));
        for (IngredientRow row : rows) {
            state.append('|').append(textOf(row.editName))
                    .append(';').append(textOf(row.editQuantity))
                    .append(';').append(row.unitIndex);
        }
        return state.toString();
    }

    private void restoreRows(Bundle state) {
        ArrayList<String> names = state.getStringArrayList(STATE_NAMES);
        ArrayList<String> quantities = state.getStringArrayList(STATE_QUANTITIES);
        int[] units = state.getIntArray(STATE_UNITS);
        if (names == null || quantities == null || units == null) {
            return;
        }
        for (int i = 0; i < names.size(); i++) {
            addIngredientRow(names.get(i), quantities.get(i), units[i]);
        }
    }

    private void saveRecipe() {
        boolean valid = true;

        String name = textOf(editName).trim();
        layoutName.setError(null);
        if (!validator.isNotBlank(name)) {
            layoutName.setError(getString(R.string.error_required));
            valid = false;
        }

        Integer portions = validator.parsePortions(textOf(editPortions));
        layoutPortions.setError(null);
        if (portions == null) {
            layoutPortions.setError(getString(R.string.error_portions_invalid, InputValidator.MAX_PORTIONS));
            valid = false;
        }

        List<Ingredient> ingredients = new ArrayList<>();
        for (IngredientRow row : rows) {
            Ingredient ingredient = readRow(row);
            if (ingredient == null) {
                valid = false;
            } else {
                ingredients.add(ingredient);
            }
        }
        if (rows.isEmpty()) {
            textIngredientsError.setVisibility(View.VISIBLE);
            valid = false;
        }

        if (!valid) {
            return;
        }

        Recipe recipe = existingRecipe != null ? existingRecipe : new Recipe();
        boolean renamed = existingRecipe != null && !name.equals(existingRecipe.getName());
        recipe.setName(name);
        recipe.setDefaultPortions(portions);
        recipe.setDescription(textOf(editDescription).trim());
        String category = editCategory.getText() != null ? editCategory.getText().toString().trim() : "";
        recipe.setCategory(category.isEmpty() ? null : category);
        recipe.setIngredients(ingredients);

        DataCallback<Void> callback = new DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                // the activity can be closed already, so use the application context
                Toast.makeText(getApplicationContext(), R.string.error_save_failed, Toast.LENGTH_LONG).show();
            }
        };
        if (existingRecipe != null) {
            recipeRepository.updateRecipe(recipe, callback);
            if (renamed) {
                // planned meals have a copy of the name, update it there too
                new MealPlanRepository().updateRecipeName(recipe.getId(), name);
            }
        } else {
            recipeRepository.addRecipe(recipe, callback);
        }
        // Firestore shows it locally right away, so we can close the screen
        finish();
    }

    // checks one row, returns the ingredient or null if something is wrong
    private Ingredient readRow(IngredientRow row) {
        boolean valid = true;
        String name = textOf(row.editName).trim();
        Double quantity = validator.parseQuantity(textOf(row.editQuantity));

        row.layoutName.setError(null);
        row.layoutQuantity.setError(null);
        row.layoutUnit.setError(null);

        if (!validator.isNotBlank(name)) {
            row.layoutName.setError(getString(R.string.error_required));
            valid = false;
        }
        if (quantity == null) {
            row.layoutQuantity.setError(getString(R.string.error_quantity_invalid, (int) InputValidator.MAX_QUANTITY));
            valid = false;
        }
        if (row.unitIndex < 0) {
            row.layoutUnit.setError(getString(R.string.error_unit_required));
            valid = false;
        }
        if (!valid) {
            return null;
        }
        return new Ingredient(name, quantity, MeasurementUnit.values()[row.unitIndex]);
    }

    private static String textOf(TextView view) {
        return view.getText() != null ? view.getText().toString() : "";
    }

    // views of one ingredient row + the chosen unit
    private static class IngredientRow {
        final View root;
        final TextInputLayout layoutName;
        final TextInputLayout layoutQuantity;
        final TextInputLayout layoutUnit;
        final TextInputEditText editName;
        final TextInputEditText editQuantity;
        final MaterialAutoCompleteTextView dropdownUnit;
        final View buttonRemove;
        int unitIndex = -1;

        IngredientRow(View root) {
            this.root = root;
            layoutName = root.findViewById(R.id.layoutIngredientName);
            layoutQuantity = root.findViewById(R.id.layoutIngredientQuantity);
            layoutUnit = root.findViewById(R.id.layoutIngredientUnit);
            editName = root.findViewById(R.id.editIngredientName);
            editQuantity = root.findViewById(R.id.editIngredientQuantity);
            dropdownUnit = root.findViewById(R.id.dropdownIngredientUnit);
            buttonRemove = root.findViewById(R.id.buttonRemoveIngredient);
        }
    }
}
