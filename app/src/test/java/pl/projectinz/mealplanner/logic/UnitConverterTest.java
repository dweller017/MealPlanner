package pl.projectinz.mealplanner.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import pl.projectinz.mealplanner.model.MeasurementUnit;
import pl.projectinz.mealplanner.model.UnitDimension;

public class UnitConverterTest {

    private static final double DELTA = 0.0001;
    private final UnitConverter converter = new UnitConverter();

    @Test
    public void convertsKilogramsToGrams() {
        assertEquals(1000.0, converter.toBaseUnit(1, MeasurementUnit.KG), DELTA);
        assertEquals(1500.0, converter.toBaseUnit(1.5, MeasurementUnit.KG), DELTA);
    }

    @Test
    public void convertsLitresToMillilitres() {
        assertEquals(250.0, converter.toBaseUnit(0.25, MeasurementUnit.L), DELTA);
    }

    @Test
    public void baseUnitsStayUnchanged() {
        assertEquals(200.0, converter.toBaseUnit(200, MeasurementUnit.G), DELTA);
        assertEquals(300.0, converter.toBaseUnit(300, MeasurementUnit.ML), DELTA);
        assertEquals(3.0, converter.toBaseUnit(3, MeasurementUnit.PCS), DELTA);
    }

    @Test
    public void convertsBetweenCompatibleUnits() {
        assertEquals(0.5, converter.convert(500, MeasurementUnit.G, MeasurementUnit.KG), DELTA);
        assertEquals(2000.0, converter.convert(2, MeasurementUnit.L, MeasurementUnit.ML), DELTA);
    }

    @Test
    public void sumOfGramsAndKilogramsInBaseUnit() {
        // 500 g + 1 kg = 1500 g
        double sum = converter.toBaseUnit(500, MeasurementUnit.G) + converter.toBaseUnit(1, MeasurementUnit.KG);
        assertEquals(1500.0, sum, DELTA);
    }

    @Test
    public void detectsCompatibleUnits() {
        assertTrue(converter.areCompatible(MeasurementUnit.G, MeasurementUnit.KG));
        assertTrue(converter.areCompatible(MeasurementUnit.ML, MeasurementUnit.L));
        assertTrue(converter.areCompatible(MeasurementUnit.PCS, MeasurementUnit.PCS));
    }

    @Test
    public void detectsIncompatibleUnits() {
        assertFalse(converter.areCompatible(MeasurementUnit.G, MeasurementUnit.PCS));
        assertFalse(converter.areCompatible(MeasurementUnit.KG, MeasurementUnit.L));
        assertFalse(converter.areCompatible(MeasurementUnit.ML, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesToConvertGramsToPieces() {
        converter.convert(100, MeasurementUnit.G, MeasurementUnit.PCS);
    }

    @Test
    public void normalizesLargeMassToKilograms() {
        Quantity result = converter.normalize(1500, UnitDimension.MASS);
        assertEquals(1.5, result.getAmount(), DELTA);
        assertEquals(MeasurementUnit.KG, result.getUnit());
    }

    @Test
    public void keepsSmallMassInGrams() {
        Quantity result = converter.normalize(999, UnitDimension.MASS);
        assertEquals(999.0, result.getAmount(), DELTA);
        assertEquals(MeasurementUnit.G, result.getUnit());
    }

    @Test
    public void normalizesExactlyOneThousandMillilitresToOneLitre() {
        Quantity result = converter.normalize(1000, UnitDimension.VOLUME);
        assertEquals(1.0, result.getAmount(), DELTA);
        assertEquals(MeasurementUnit.L, result.getUnit());
    }

    @Test
    public void piecesAreNeverChangedIntoAnotherUnit() {
        Quantity result = converter.normalize(1200, UnitDimension.COUNT);
        assertEquals(1200.0, result.getAmount(), DELTA);
        assertEquals(MeasurementUnit.PCS, result.getUnit());
    }
}
