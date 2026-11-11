package pl.projectinz.mealplanner.ui.recipes;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.data.RecipeRepository;
import pl.projectinz.mealplanner.logic.RecipeFilter;
import pl.projectinz.mealplanner.model.Recipe;

// Recipes tab: list of recipes with search and favourites filter
public class RecipesFragment extends Fragment implements RecipeAdapter.Listener {

    private final RecipeRepository recipeRepository = new RecipeRepository();
    private final RecipeFilter recipeFilter = new RecipeFilter();

    // all recipes from Firestore, the filter works on this list
    private List<Recipe> allRecipes = new ArrayList<>();
    private ListenerRegistration recipesListener;

    private RecipeAdapter adapter;
    private TextInputEditText editSearch;
    private Chip chipFavourites;
    private TextView textEmpty;
    private View progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editSearch = view.findViewById(R.id.editSearch);
        chipFavourites = view.findViewById(R.id.chipFavourites);
        textEmpty = view.findViewById(R.id.textEmpty);
        progress = view.findViewById(R.id.progress);

        adapter = new RecipeAdapter(this);
        RecyclerView recycler = view.findViewById(R.id.recyclerRecipes);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                showFilteredRecipes();
            }
        });
        chipFavourites.setOnCheckedChangeListener((chip, checked) -> showFilteredRecipes());

        view.findViewById(R.id.fabAddRecipe).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddEditRecipeActivity.class)));
    }

    @Override
    public void onStart() {
        super.onStart();
        // start listening when the screen becomes visible
        recipesListener = recipeRepository.listenToRecipes(new DataCallback<List<Recipe>>() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                allRecipes = recipes;
                progress.setVisibility(View.GONE);
                showFilteredRecipes();
            }

            @Override
            public void onError(Exception e) {
                progress.setVisibility(View.GONE);
                showError();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // stop listening when the screen is not visible
        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }
    }

    private void showFilteredRecipes() {
        String query = editSearch.getText() != null ? editSearch.getText().toString() : "";
        List<Recipe> filtered = recipeFilter.filter(allRecipes, query, chipFavourites.isChecked());
        adapter.setRecipes(filtered);

        if (!filtered.isEmpty()) {
            textEmpty.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.VISIBLE);
            textEmpty.setText(allRecipes.isEmpty() ? R.string.recipes_empty : R.string.recipes_no_results);
        }
    }

    @Override
    public void onRecipeClicked(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailsActivity.class);
        intent.putExtra(RecipeDetailsActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }

    @Override
    public void onFavouriteClicked(Recipe recipe) {
        // the listener refreshes the list by itself
        recipeRepository.setFavourite(recipe.getId(), !recipe.isFavourite(), new DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                showError();
            }
        });
    }

    private void showError() {
        if (getView() != null) {
            Snackbar.make(getView(), R.string.error_generic, Snackbar.LENGTH_LONG).show();
        }
    }
}
