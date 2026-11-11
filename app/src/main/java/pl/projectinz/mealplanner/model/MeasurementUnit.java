package pl.projectinz.mealplanner.model;

// units that the app supports
//
// each unit knows its dimension and how many base units it has
// (base units: gram, millilitre, piece), e.g. KG = 1000 G
// Firestore saves the enum as its name, e.g. "KG"
public enum MeasurementUnit {
    G(UnitDimension.MASS, 1),
    KG(UnitDimension.MASS, 1000),
    ML(UnitDimension.VOLUME, 1),
    L(UnitDimension.VOLUME, 1000),
    PCS(UnitDimension.COUNT, 1);

    private final UnitDimension dimension;
    private final double factorToBase;

    MeasurementUnit(UnitDimension dimension, double factorToBase) {
        this.dimension = dimension;
        this.factorToBase = factorToBase;
    }

    public UnitDimension getDimension() {
        return dimension;
    }

    public double getFactorToBase() {
        return factorToBase;
    }
}
