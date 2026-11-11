package pl.projectinz.mealplanner.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

// profile of a user, path: users/{uid} (uid from Firebase Authentication)
public class User {

    @DocumentId
    private String uid;

    private String email;
    private String displayName;

    // date range of the current shopping list, null before the first generate
    private String shoppingListFrom;
    private String shoppingListTo;

    @ServerTimestamp
    private Date createdAt;

    // empty constructor is needed by Firestore
    public User() {
    }

    public User(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getShoppingListFrom() {
        return shoppingListFrom;
    }

    public void setShoppingListFrom(String shoppingListFrom) {
        this.shoppingListFrom = shoppingListFrom;
    }

    public String getShoppingListTo() {
        return shoppingListTo;
    }

    public void setShoppingListTo(String shoppingListTo) {
        this.shoppingListTo = shoppingListTo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
