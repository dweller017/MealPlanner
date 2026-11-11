package pl.projectinz.mealplanner.ui.common;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

// switches the app language (English / Polish)
//
// uses the per-app language API from AppCompat
// on Android 13+ the system saves the choice, on older ones AppCompat does
// (see AppLocalesMetadataHolderService in the manifest)
// AppCompat recreates the open screens by itself
public final class LanguageHelper {

    public static final String ENGLISH = "en";
    public static final String POLISH = "pl";

    private LanguageHelper() {
    }

    public static void setLanguage(String languageTag) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
    }

    // "pl" if Polish is chosen, otherwise "en"
    public static String getCurrentLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (!locales.isEmpty() && locales.get(0) != null
                && POLISH.equals(locales.get(0).getLanguage())) {
            return POLISH;
        }
        return ENGLISH;
    }
}
