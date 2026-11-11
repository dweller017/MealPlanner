package pl.projectinz.mealplanner.ui.common;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pl.projectinz.mealplanner.R;

// asks "Discard changes?" when the user leaves a form with unsaved changes
// (back button, back gesture or the toolbar arrow)
//
// the form gives all its values as one string. we remember it when the form
// is filled and compare it when the user wants to leave
public class UnsavedChangesGuard {

    // the form returns all its values as one string
    public interface FormState {
        String getFormState();
    }

    private static final String STATE_BASELINE = "unsavedChangesBaseline";

    private final AppCompatActivity activity;
    private final FormState formState;
    // form state after filling it, null = form not ready yet
    private String baseline;

    public UnsavedChangesGuard(AppCompatActivity activity, FormState formState) {
        this.activity = activity;
        this.formState = formState;
        // OnBackPressedCallback is the new way to handle back (onBackPressed is deprecated)
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requestClose();
            }
        });
    }

    // call this when the form is filled with its start values
    public void markClean() {
        baseline = formState.getFormState();
    }

    public void saveState(Bundle outState) {
        outState.putString(STATE_BASELINE, baseline);
    }

    public void restoreState(Bundle savedState) {
        if (savedState != null) {
            baseline = savedState.getString(STATE_BASELINE);
        }
    }

    public boolean hasChanges() {
        return baseline != null && !baseline.equals(formState.getFormState());
    }

    // closes the screen, asks first if something changed
    public void requestClose() {
        if (!hasChanges()) {
            activity.finish();
            return;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.discard_changes_title)
                .setMessage(R.string.discard_changes_message)
                .setNegativeButton(R.string.action_keep_editing, null)
                .setPositiveButton(R.string.action_discard, (dialog, which) -> activity.finish())
                .show();
    }
}
