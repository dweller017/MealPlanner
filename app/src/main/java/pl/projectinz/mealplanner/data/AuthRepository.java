package pl.projectinz.mealplanner.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import pl.projectinz.mealplanner.model.User;

// login, register and logout with Firebase Authentication
// activities use this class instead of calling FirebaseAuth directly
public class AuthRepository {

    private final FirebaseAuth auth;
    private final UserRepository userRepository;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.userRepository = new UserRepository();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    // returns null if nobody is logged in
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public String getCurrentUserName() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }

    public void login(String email, String password, final DataCallback<Void> callback) {
        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // creates the account, saves the name and creates users/{uid} in Firestore
    public void register(final String email, String password, final String displayName,
                         final DataCallback<Void> callback) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError(new IllegalStateException("User not created"));
                        return;
                    }
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build();
                    userRepository.createProfile(firebaseUser.getUid(), new User(email.trim(), displayName));
                    // wait for the name to be saved so Home can show it
                    // (if it fails the account still exists, so we go on anyway)
                    firebaseUser.updateProfile(profile)
                            .addOnCompleteListener(task -> callback.onSuccess(null));
                })
                .addOnFailureListener(callback::onError);
    }

    // sends an email with a link to set a new password
    // languageTag ("en" / "pl") decides the language of the email
    public void sendPasswordReset(String email, String languageTag, final DataCallback<Void> callback) {
        auth.setLanguageCode(languageTag);
        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void logout() {
        auth.signOut();
    }
}
