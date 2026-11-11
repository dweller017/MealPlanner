package pl.projectinz.mealplanner.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.AuthRepository;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.logic.InputValidator;
import pl.projectinz.mealplanner.ui.common.EdgeToEdgeHelper;
import pl.projectinz.mealplanner.ui.common.LanguageHelper;
import pl.projectinz.mealplanner.ui.main.MainActivity;

// first screen of the app
// if the user is already logged in we go straight to MainActivity
public class LoginActivity extends AppCompatActivity {

    private final AuthRepository authRepository = new AuthRepository();
    private final InputValidator validator = new InputValidator();

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private MaterialButton buttonLogin;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase remembers the logged-in user between app starts
        if (authRepository.isLoggedIn()) {
            openMainScreen();
            return;
        }

        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_login);
        EdgeToEdgeHelper.applyPadding(findViewById(R.id.root));

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        progress = findViewById(R.id.progress);

        buttonLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.buttonGoToRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.buttonForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());
    }

    // dialog where the user types the email and gets a reset link
    private void showForgotPasswordDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);
        final TextInputLayout layoutResetEmail = view.findViewById(R.id.layoutResetEmail);
        final TextInputEditText editResetEmail = view.findViewById(R.id.editResetEmail);
        // use the email from the login form, so the user doesn't have to type it again
        editResetEmail.setText(textOf(editEmail).trim());

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_forgot_password)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_send_link, null)
                .show();

        // own click listener so the dialog stays open when something is wrong
        final Button buttonSend = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        buttonSend.setOnClickListener(v -> {
            String email = textOf(editResetEmail).trim();
            layoutResetEmail.setError(null);
            if (!validator.isValidEmail(email)) {
                layoutResetEmail.setError(getString(R.string.error_invalid_email));
                return;
            }
            buttonSend.setEnabled(false);
            authRepository.sendPasswordReset(email, LanguageHelper.getCurrentLanguage(), new DataCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    dialog.dismiss();
                    showResetEmailSent(email);
                }

                @Override
                public void onError(Exception e) {
                    buttonSend.setEnabled(true);
                    layoutResetEmail.setError(getString(AuthErrorMapper.getMessage(e)));
                }
            });
        });
    }

    // Firebase doesn't tell us if the account exists (for security),
    // so the message says "if an account exists"
    private void showResetEmailSent(String email) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_email_sent_title)
                .setMessage(getString(R.string.reset_email_sent_message, email))
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    private void attemptLogin() {
        String email = textOf(editEmail);
        String password = textOf(editPassword);

        // 1. validate input
        boolean valid = true;
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        if (!validator.isValidEmail(email)) {
            layoutEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }
        if (!validator.isNotBlank(password)) {
            layoutPassword.setError(getString(R.string.error_required));
            valid = false;
        }
        if (!valid) {
            return;
        }

        // 2. call Firebase
        setLoading(true);
        authRepository.login(email, password, new DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                openMainScreen();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Snackbar.make(buttonLogin, AuthErrorMapper.getMessage(e), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!loading);
    }

    private void openMainScreen() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
