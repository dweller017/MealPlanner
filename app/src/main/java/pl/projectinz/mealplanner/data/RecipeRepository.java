package pl.projectinz.mealplanner.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.projectinz.mealplanner.model.Recipe;

// recipes of the logged in user: users/{uid}/recipes
//
// Firestore shows changes right away from its local cache (also offline),
// onSuccess comes only after the server saves it, so screens don't wait for it
public class RecipeRepository {

    private final CollectionReference recipes;

    public RecipeRepository() {
        String uid = FirebaseAuth.getInstance().getUid();
        this.recipes = FirestorePaths.recipes(uid);
    }

    // listens to all recipes, called again after every change
    // remember to call remove() when the screen is not visible
    public ListenerRegistration listenToRecipes(final DataCallback<List<Recipe>> callback) {
        return recipes.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            List<Recipe> result = new ArrayList<>();
            if (snapshot != null) {
                for (QueryDocumentSnapshot document : snapshot) {
                    result.add(document.toObject(Recipe.class));
                }
            }
            callback.onSuccess(result);
        });
    }

    // listens to one recipe, gives null if it was deleted
    public ListenerRegistration listenToRecipe(String recipeId, final DataCallback<Recipe> callback) {
        return recipes.document(recipeId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            callback.onSuccess(toRecipe(snapshot));
        });
    }

    // reads one recipe once (for the edit screen)
    public void getRecipe(String recipeId, final DataCallback<Recipe> callback) {
        recipes.document(recipeId).get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(toRecipe(snapshot)))
                .addOnFailureListener(callback::onError);
    }

    // reads all recipes as a map id -> recipe (for the shopping list)
    public void getAllRecipesById(final DataCallback<Map<String, Recipe>> callback) {
        recipes.get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Recipe> result = new HashMap<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        Recipe recipe = document.toObject(Recipe.class);
                        result.put(recipe.getId(), recipe);
                    }
                    callback.onSuccess(result);
                })
                .addOnFailureListener(callback::onError);
    }

    // adds a new recipe, Firestore makes the id
    public String addRecipe(Recipe recipe, final DataCallback<Void> callback) {
        DocumentReference document = recipes.document();
        document.set(recipe)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
        return document.getId();
    }

    // overwrites a recipe (id must be set)
    public void updateRecipe(Recipe recipe, final DataCallback<Void> callback) {
        recipes.document(recipe.getId()).set(recipe)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // changes only the favourite field
    public void setFavourite(String recipeId, boolean favourite, final DataCallback<Void> callback) {
        recipes.document(recipeId).update("favourite", favourite)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void deleteRecipe(String recipeId, final DataCallback<Void> callback) {
        recipes.document(recipeId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    private static Recipe toRecipe(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return snapshot.toObject(Recipe.class);
    }
}
