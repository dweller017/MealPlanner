package pl.projectinz.mealplanner.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.AuthRepository;
import pl.projectinz.mealplanner.ui.auth.LoginActivity;
import pl.projectinz.mealplanner.ui.common.EdgeToEdgeHelper;
import pl.projectinz.mealplanner.ui.common.LanguageHelper;
import pl.projectinz.mealplanner.ui.common.ThemeHelper;

// settings: account info, language, theme and logout
public class SettingsActivity extends AppCompatActivity {

    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_settings);
        EdgeToEdgeHelper.applyPadding(findViewById(R.id.root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView textUserName = findViewById(R.id.textUserName);
        TextView textUserEmail = findViewById(R.id.textUserEmail);
        textUserName.setText(authRepository.getCurrentUserName());
        textUserEmail.setText(authRepository.getCurrentUserEmail());

        setupLanguage();
        setupTheme();

        findViewById(R.id.buttonLogout).setOnClickListener(v -> confirmLogout());
    }

    private void setupLanguage() {
        RadioGroup radioLanguage = findViewById(R.id.radioLanguage);
        // check the current language first, then listen for changes
        if (LanguageHelper.POLISH.equals(LanguageHelper.getCurrentLanguage())) {
            radioLanguage.check(R.id.radioPolish);
        } else {
            radioLanguage.check(R.id.radioEnglish);
        }
        radioLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String language = checkedId == R.id.radioPolish ? LanguageHelper.POLISH : LanguageHelper.ENGLISH;
            if (!language.equals(LanguageHelper.getCurrentLanguage())) {
                // AppCompat recreates the screens with the new language
                LanguageHelper.setLanguage(language);
            }
        });
    }

    private void setupTheme() {
        RadioGroup radioTheme = findViewById(R.id.radioTheme);
        // check the saved theme first, then listen for changes
        String saved = ThemeHelper.getSavedTheme(this);
        if (ThemeHelper.LIGHT.equals(saved)) {
            radioTheme.check(R.id.radioThemeLight);
        } else if (ThemeHelper.DARK.equals(saved)) {
            radioTheme.check(R.id.radioThemeDark);
        } else {
            radioTheme.check(R.id.radioThemeSystem);
        }
        radioTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String theme;
            if (checkedId == R.id.radioThemeLight) {
                theme = ThemeHelper.LIGHT;
            } else if (checkedId == R.id.radioThemeDark) {
                theme = ThemeHelper.DARK;
            } else {
                theme = ThemeHelper.SYSTEM;
            }
            if (!theme.equals(ThemeHelper.getSavedTheme(this))) {
                // AppCompat redraws the screens with the new theme
                ThemeHelper.setTheme(this, theme);
            }
        });
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_logout)
                .setMessage(R.string.logout_confirm)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_logout, (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        authRepository.logout();
        // open login and remove all other screens
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
