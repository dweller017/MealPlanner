package pl.projectinz.mealplanner.model;

// one ingredient of a recipe, e.g. Chicken 200 g
// saved inside the recipe document, so it has no own id
public class Ingredient {

    private String name;
    private double quantity;
    private MeasurementUnit unit;

    // empty constructor is needed by Firestore
    public Ingredient() {
    }

    public Ingredient(String name, double quantity, MeasurementUnit unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
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
}
