package pl.projectinz.mealplanner.ui.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

// switches between light and dark theme
// saved in SharedPreferences because it's a setting of the phone,
// not user data, so it doesn't go to Firestore
public final class ThemeHelper {

    public static final String SYSTEM = "system";
    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    private static final String PREFERENCES = "settings";
    private static final String KEY_THEME = "theme";

    private ThemeHelper() {
    }

    // called once when the app starts (MealPlannerApp)
    public static void applySavedTheme(Context context) {
        apply(getSavedTheme(context));
    }

    public static void setTheme(Context context, String theme) {
        preferences(context).edit().putString(KEY_THEME, theme).apply();
        apply(theme);
    }

    public static String getSavedTheme(Context context) {
        return preferences(context).getString(KEY_THEME, SYSTEM);
    }

    private static void apply(String theme) {
        if (LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            // follow the phone setting
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
