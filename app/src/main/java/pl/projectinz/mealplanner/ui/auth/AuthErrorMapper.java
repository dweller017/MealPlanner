package pl.projectinz.mealplanner.ui.auth;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import pl.projectinz.mealplanner.R;

// changes Firebase login errors into messages the user understands
//
// new Firebase projects don't say if the email or the password was wrong
// (email enumeration protection), so both end up as "invalid credentials"
public final class AuthErrorMapper {

    private AuthErrorMapper() {
    }

    // returns the string id of the message
    public static int getMessage(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return R.string.error_auth_network;
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return R.string.error_auth_too_many_requests;
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return R.string.error_auth_email_in_use;
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return R.string.error_auth_weak_password;
        }
        if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) {
            return R.string.error_auth_invalid_credentials;
        }
        return R.string.error_generic;
    }
}
