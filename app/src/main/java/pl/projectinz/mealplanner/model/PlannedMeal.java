package pl.projectinz.mealplanner.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

// a recipe planned for a day with some number of portions
// path: users/{uid}/mealPlans/{id}
//
// date is text "yyyy-MM-dd" so it sorts correctly in queries
// the recipe name is copied here so the planner doesn't need to load the recipe
public class PlannedMeal {

    @DocumentId
    private String id;

    private String recipeId;
    private String recipeName;
    private String date;
    private int portions;

    @ServerTimestamp
    private Date createdAt;

    // empty constructor is needed by Firestore
    public PlannedMeal() {
    }

    public PlannedMeal(String recipeId, String recipeName, String date, int portions) {
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.date = date;
        this.portions = portions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getPortions() {
        return portions;
    }

    public void setPortions(int portions) {
        this.portions = portions;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
