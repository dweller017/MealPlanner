package pl.projectinz.mealplanner;

import android.app.Application;

import pl.projectinz.mealplanner.ui.common.ThemeHelper;

// runs once when the app starts, before any screen
// sets the saved theme (light/dark) so the first screen already has it
public class MealPlannerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applySavedTheme(this);
    }
}
