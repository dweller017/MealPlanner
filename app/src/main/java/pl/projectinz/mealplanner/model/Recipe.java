package pl.projectinz.mealplanner.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// a recipe of the user, path: users/{uid}/recipes/{id}
// ingredients are saved inside as a list
public class Recipe {

    // Firestore puts the document id here (it's not saved as a field)
    @DocumentId
    private String id;

    private String name;
    private String description;
    private int defaultPortions;
    private String category;
    private boolean favourite;
    private List<Ingredient> ingredients = new ArrayList<>();

    // the server sets this when the document is created
    @ServerTimestamp
    private Date createdAt;

    // empty constructor is needed by Firestore
    public Recipe() {
    }

    public Recipe(String name, String description, int defaultPortions, List<Ingredient> ingredients) {
        this.name = name;
        this.description = description;
        this.defaultPortions = defaultPortions;
        this.ingredients = ingredients;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDefaultPortions() {
        return defaultPortions;
    }

    public void setDefaultPortions(int defaultPortions) {
        this.defaultPortions = defaultPortions;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
