package pl.projectinz.mealplanner.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.AuthRepository;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.logic.InputValidator;
import pl.projectinz.mealplanner.ui.common.EdgeToEdgeHelper;
import pl.projectinz.mealplanner.ui.main.MainActivity;

// register screen: name, email and password
public class RegisterActivity extends AppCompatActivity {

    private final AuthRepository authRepository = new AuthRepository();
    private final InputValidator validator = new InputValidator();

    private TextInputLayout layoutName;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputLayout layoutPasswordRepeat;
    private TextInputEditText editName;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextInputEditText editPasswordRepeat;
    private MaterialButton buttonRegister;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_register);
        EdgeToEdgeHelper.applyPadding(findViewById(R.id.root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutName = findViewById(R.id.layoutName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutPasswordRepeat = findViewById(R.id.layoutPasswordRepeat);
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editPasswordRepeat = findViewById(R.id.editPasswordRepeat);
        buttonRegister = findViewById(R.id.buttonRegister);
        progress = findViewById(R.id.progress);

        buttonRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String name = textOf(editName).trim();
        String email = textOf(editEmail);
        String password = textOf(editPassword);
        String passwordRepeat = textOf(editPasswordRepeat);

        layoutName.setError(null);
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutPasswordRepeat.setError(null);

        boolean valid = true;
        if (!validator.isNotBlank(name)) {
            layoutName.setError(getString(R.string.error_required));
            valid = false;
        }
        if (!validator.isValidEmail(email)) {
            layoutEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }
        if (!validator.isValidPassword(password)) {
            layoutPassword.setError(getResources().getQuantityString(R.plurals.error_password_too_short,
                    InputValidator.MIN_PASSWORD_LENGTH, InputValidator.MIN_PASSWORD_LENGTH));
            valid = false;
        }
        if (!password.equals(passwordRepeat)) {
            layoutPasswordRepeat.setError(getString(R.string.error_passwords_not_matching));
            valid = false;
        }
        if (!valid) {
            return;
        }

        setLoading(true);
        authRepository.register(email, password, name, new DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // clear the back stack so back doesn't go to the login screen
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Snackbar.make(buttonRegister, AuthErrorMapper.getMessage(e), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(!loading);
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
