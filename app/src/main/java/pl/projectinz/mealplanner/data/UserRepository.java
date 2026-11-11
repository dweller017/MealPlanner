package pl.projectinz.mealplanner.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import pl.projectinz.mealplanner.model.User;

// profile document users/{uid}
public class UserRepository {

    public static final String FIELD_SHOPPING_FROM = "shoppingListFrom";
    public static final String FIELD_SHOPPING_TO = "shoppingListTo";

    // saves the profile after registration
    // we don't wait for the server here, Firestore sends it when it can
    public void createProfile(String uid, User user) {
        FirestorePaths.user(uid).set(user, SetOptions.merge());
    }

    // saves the date range of the shopping list
    // merge() changes only these fields, the rest of the profile stays
    public void saveShoppingRange(String fromDate, String toDate) {
        Map<String, Object> fields = new HashMap<>();
        fields.put(FIELD_SHOPPING_FROM, fromDate);
        fields.put(FIELD_SHOPPING_TO, toDate);
        FirestorePaths.user(currentUid()).set(fields, SetOptions.merge());
    }

    // gives {from, to} or null if the list was never generated
    public void getShoppingRange(final DataCallback<String[]> callback) {
        FirestorePaths.user(currentUid()).get()
                .addOnSuccessListener(snapshot -> {
                    String from = snapshot.getString(FIELD_SHOPPING_FROM);
                    String to = snapshot.getString(FIELD_SHOPPING_TO);
                    callback.onSuccess(from != null && to != null ? new String[]{from, to} : null);
                })
                .addOnFailureListener(callback::onError);
    }

    private static String currentUid() {
        return FirebaseAuth.getInstance().getUid();
    }
}
