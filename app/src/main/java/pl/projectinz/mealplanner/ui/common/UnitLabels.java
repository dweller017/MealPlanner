package pl.projectinz.mealplanner.ui.common;

import android.content.Context;

import java.util.Locale;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.logic.QuantityFormatter;
import pl.projectinz.mealplanner.model.MeasurementUnit;

// changes units into text for the screen
// the database has "PCS", the user sees "pcs" or "szt."
public final class UnitLabels {

    private static final QuantityFormatter FORMATTER = new QuantityFormatter();

    private UnitLabels() {
    }

    public static String getLabel(Context context, MeasurementUnit unit) {
        if (unit == null) {
            return "";
        }
        switch (unit) {
            case G:
                return context.getString(R.string.unit_g);
            case KG:
                return context.getString(R.string.unit_kg);
            case ML:
                return context.getString(R.string.unit_ml);
            case L:
                return context.getString(R.string.unit_l);
            default:
                return context.getString(R.string.unit_pcs);
        }
    }

    // all labels in the same order as MeasurementUnit.values() (for the dropdowns)
    public static String[] getAllLabels(Context context) {
        MeasurementUnit[] units = MeasurementUnit.values();
        String[] labels = new String[units.length];
        for (int i = 0; i < units.length; i++) {
            labels[i] = getLabel(context, units[i]);
        }
        return labels;
    }

    // e.g. "1.5 kg" in English or "1,5 kg" in Polish
    public static String formatQuantity(Context context, double quantity, MeasurementUnit unit) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return FORMATTER.format(quantity, locale) + " " + getLabel(context, unit);
    }
}
