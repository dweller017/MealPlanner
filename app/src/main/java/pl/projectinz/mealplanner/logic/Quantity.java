package pl.projectinz.mealplanner.logic;

import pl.projectinz.mealplanner.model.MeasurementUnit;

// amount + unit, e.g. 1.5 KG
public class Quantity {

    private final double amount;
    private final MeasurementUnit unit;

    public Quantity(double amount, MeasurementUnit unit) {
        this.amount = amount;
        this.unit = unit;
    }

    public double getAmount() {
        return amount;
    }

    public MeasurementUnit getUnit() {
        return unit;
    }
}
