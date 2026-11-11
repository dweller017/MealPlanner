package pl.projectinz.mealplanner.ui.recipes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.model.Recipe;

// shows recipes in a RecyclerView, clicks go to the fragment through Listener
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface Listener {
        void onRecipeClicked(Recipe recipe);

        void onFavouriteClicked(Recipe recipe);
    }

    private final List<Recipe> recipes = new ArrayList<>();
    private final Listener listener;

    public RecipeAdapter(Listener listener) {
        this.listener = listener;
    }

    // replaces the whole list (fine for a small list)
    @SuppressLint("NotifyDataSetChanged")
    public void setRecipes(List<Recipe> newRecipes) {
        recipes.clear();
        recipes.addAll(newRecipes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        holder.bind(recipes.get(position));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textInfo;
        private final ImageButton buttonFavourite;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textInfo = itemView.findViewById(R.id.textInfo);
            buttonFavourite = itemView.findViewById(R.id.buttonFavourite);
        }

        void bind(final Recipe recipe) {
            Context context = itemView.getContext();
            textName.setText(recipe.getName());

            int ingredientCount = recipe.getIngredients() != null ? recipe.getIngredients().size() : 0;
            String info = context.getResources().getQuantityString(R.plurals.portions_count,
                    recipe.getDefaultPortions(), recipe.getDefaultPortions())
                    + " • " + context.getResources().getQuantityString(R.plurals.ingredients_count,
                    ingredientCount, ingredientCount);
            if (recipe.getCategory() != null && !recipe.getCategory().isEmpty()) {
                info = recipe.getCategory() + " • " + info;
            }
            textInfo.setText(info);

            buttonFavourite.setImageResource(recipe.isFavourite() ? R.drawable.ic_star : R.drawable.ic_star_border);
            buttonFavourite.setOnClickListener(v -> listener.onFavouriteClicked(recipe));
            itemView.setOnClickListener(v -> listener.onRecipeClicked(recipe));
        }
    }
}
