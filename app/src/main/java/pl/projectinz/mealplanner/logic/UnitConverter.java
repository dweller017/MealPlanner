package pl.projectinz.mealplanner.logic;

import pl.projectinz.mealplanner.model.MeasurementUnit;
import pl.projectinz.mealplanner.model.UnitDimension;

// converts between units of the same kind
//
// every quantity can be changed to its base unit (g, ml or pcs)
// and then simply added. g and pcs are never converted.
public class UnitConverter {

    // from 1000 we show kg / l instead of g / ml
    private static final double BIG_UNIT_THRESHOLD = 1000;

    // e.g. 1.5 KG -> 1500 (grams)
    public double toBaseUnit(double quantity, MeasurementUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit cannot be null");
        }
        return quantity * unit.getFactorToBase();
    }

    // true if both units measure the same thing (e.g. G and KG)
    public boolean areCompatible(MeasurementUnit first, MeasurementUnit second) {
        if (first == null || second == null) {
            return false;
        }
        return first.getDimension() == second.getDimension();
    }

    // e.g. 500 G -> 0.5 KG, throws an exception if the units don't match
    public double convert(double quantity, MeasurementUnit from, MeasurementUnit to) {
        if (!areCompatible(from, to)) {
            throw new IllegalArgumentException("Cannot convert " + from + " to " + to);
        }
        return toBaseUnit(quantity, from) / to.getFactorToBase();
    }

    // base unit of a dimension: G, ML or PCS
    public MeasurementUnit getBaseUnit(UnitDimension dimension) {
        switch (dimension) {
            case MASS:
                return MeasurementUnit.G;
            case VOLUME:
                return MeasurementUnit.ML;
            default:
                return MeasurementUnit.PCS;
        }
    }

    // chooses a nicer unit, e.g. 1500 g -> 1.5 kg, 2000 ml -> 2 l
    public Quantity normalize(double baseQuantity, UnitDimension dimension) {
        if (dimension == UnitDimension.MASS && baseQuantity >= BIG_UNIT_THRESHOLD) {
            return new Quantity(baseQuantity / MeasurementUnit.KG.getFactorToBase(), MeasurementUnit.KG);
        }
        if (dimension == UnitDimension.VOLUME && baseQuantity >= BIG_UNIT_THRESHOLD) {
            return new Quantity(baseQuantity / MeasurementUnit.L.getFactorToBase(), MeasurementUnit.L);
        }
        return new Quantity(baseQuantity, getBaseUnit(dimension));
    }
}
