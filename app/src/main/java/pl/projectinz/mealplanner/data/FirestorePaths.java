package pl.projectinz.mealplanner.data;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

// all Firestore paths are in one place:
// users/{uid}
// users/{uid}/recipes
// users/{uid}/mealPlans
// users/{uid}/shoppingItems
public final class FirestorePaths {

    public static final String USERS = "users";
    public static final String RECIPES = "recipes";
    public static final String MEAL_PLANS = "mealPlans";
    public static final String SHOPPING_ITEMS = "shoppingItems";

    private FirestorePaths() {
    }

    public static DocumentReference user(String uid) {
        return FirebaseFirestore.getInstance().collection(USERS).document(uid);
    }

    public static CollectionReference recipes(String uid) {
        return user(uid).collection(RECIPES);
    }

    public static CollectionReference mealPlans(String uid) {
        return user(uid).collection(MEAL_PLANS);
    }

    public static CollectionReference shoppingItems(String uid) {
        return user(uid).collection(SHOPPING_ITEMS);
    }
}
