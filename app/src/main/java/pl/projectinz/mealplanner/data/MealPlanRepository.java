package pl.projectinz.mealplanner.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.projectinz.mealplanner.model.PlannedMeal;

// planned meals of the logged in user: users/{uid}/mealPlans
// dates are saved as "yyyy-MM-dd" text so a date range is just a simple query
public class MealPlanRepository {

    private static final String FIELD_DATE = "date";
    private static final String FIELD_RECIPE_ID = "recipeId";
    private static final String FIELD_RECIPE_NAME = "recipeName";

    private final CollectionReference mealPlans;

    public MealPlanRepository() {
        String uid = FirebaseAuth.getInstance().getUid();
        this.mealPlans = FirestorePaths.mealPlans(uid);
    }

    private Query between(String fromDate, String toDate) {
        return mealPlans
                .whereGreaterThanOrEqualTo(FIELD_DATE, fromDate)
                .whereLessThanOrEqualTo(FIELD_DATE, toDate);
    }

    // listens to meals between two dates (both included)
    public ListenerRegistration listenToMealsBetween(String fromDate, String toDate,
                                                     final DataCallback<List<PlannedMeal>> callback) {
        return between(fromDate, toDate).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            callback.onSuccess(toSortedList(snapshot));
        });
    }

    // same, but reads only once (for the shopping list)
    public void getMealsBetween(String fromDate, String toDate, final DataCallback<List<PlannedMeal>> callback) {
        between(fromDate, toDate).get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(toSortedList(snapshot)))
                .addOnFailureListener(callback::onError);
    }

    public void getMeal(String mealId, final DataCallback<PlannedMeal> callback) {
        mealPlans.document(mealId).get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(toMeal(snapshot)))
                .addOnFailureListener(callback::onError);
    }

    public void addMeal(PlannedMeal meal, final DataCallback<Void> callback) {
        DocumentReference document = mealPlans.document();
        document.set(meal)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void updateMeal(PlannedMeal meal, final DataCallback<Void> callback) {
        mealPlans.document(meal.getId()).set(meal)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void deleteMeal(String mealId, final DataCallback<Void> callback) {
        mealPlans.document(mealId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // when a recipe gets a new name, change it in its planned meals too
    public void updateRecipeName(String recipeId, final String newName) {
        mealPlans.whereEqualTo(FIELD_RECIPE_ID, recipeId).get().addOnSuccessListener(snapshot -> {
            WriteBatch batch = FirebaseFirestore.getInstance().batch();
            for (QueryDocumentSnapshot document : snapshot) {
                batch.update(document.getReference(), FIELD_RECIPE_NAME, newName);
            }
            batch.commit();
        });
    }

    // when a recipe is deleted, delete its planned meals
    public void deleteMealsForRecipe(String recipeId) {
        mealPlans.whereEqualTo(FIELD_RECIPE_ID, recipeId).get().addOnSuccessListener(snapshot -> {
            WriteBatch batch = FirebaseFirestore.getInstance().batch();
            for (QueryDocumentSnapshot document : snapshot) {
                batch.delete(document.getReference());
            }
            batch.commit();
        });
    }

    private static List<PlannedMeal> toSortedList(QuerySnapshot snapshot) {
        List<PlannedMeal> result = new ArrayList<>();
        if (snapshot != null) {
            for (QueryDocumentSnapshot document : snapshot) {
                result.add(document.toObject(PlannedMeal.class));
            }
        }
        Collections.sort(result, (a, b) -> {
            int byDate = a.getDate().compareTo(b.getDate());
            if (byDate != 0) {
                return byDate;
            }
            return String.valueOf(a.getRecipeName()).compareToIgnoreCase(String.valueOf(b.getRecipeName()));
        });
        return result;
    }

    private static PlannedMeal toMeal(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return snapshot.toObject(PlannedMeal.class);
    }
}
