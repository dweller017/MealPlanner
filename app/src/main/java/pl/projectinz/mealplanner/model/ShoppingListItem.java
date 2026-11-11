package pl.projectinz.mealplanner.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

// one product on the shopping list, path: users/{uid}/shoppingItems/{id}
public class ShoppingListItem {

    @DocumentId
    private String id;

    private String name;
    private double quantity;
    private MeasurementUnit unit;
    private String category;
    private boolean purchased;
    private ItemSource source;

    @ServerTimestamp
    private Date createdAt;

    // empty constructor is needed by Firestore
    public ShoppingListItem() {
    }

    public ShoppingListItem(String name, double quantity, MeasurementUnit unit, ItemSource source) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.source = source;
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

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public MeasurementUnit getUnit() {
        return unit;
    }

    public void setUnit(MeasurementUnit unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public ItemSource getSource() {
        return source;
    }

    public void setSource(ItemSource source) {
        this.source = source;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
