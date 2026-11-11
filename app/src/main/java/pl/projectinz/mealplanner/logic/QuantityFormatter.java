package pl.projectinz.mealplanner.logic;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

// formats a number for the screen: max 2 decimals, no extra zeros
// 200.0 -> "200", 1.5 -> "1.5" (in Polish "1,5")
public class QuantityFormatter {

    public String format(double quantity, Locale locale) {
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale));
        return format.format(quantity);
    }
}
