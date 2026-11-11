package pl.projectinz.mealplanner.ui.common;

import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// from Android 15 the app is drawn under the status bar, nav bar and keyboard
// this adds padding to the root view so nothing is hidden
//
// use it in onCreate:
// EdgeToEdgeHelper.enable(this);
// setContentView(...);
// EdgeToEdgeHelper.applyPadding(findViewById(R.id.root));
public final class EdgeToEdgeHelper {

    private EdgeToEdgeHelper() {
    }

    public static void enable(ComponentActivity activity) {
        EdgeToEdge.enable(activity);
    }

    public static void applyPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(bars.bottom, keyboard.bottom);
            view.setPadding(bars.left, bars.top, bars.right, bottom);
            // insets are handled here, so the child views shouldn't add them again
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
