package pl.projectinz.mealplanner.ui.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

// checks if the phone is online
// the app works offline too (Firestore caches), but the user should know
// that changes are not synced yet. call start() in onStart and stop() in onStop
public class NetworkMonitor {

    public interface Listener {
        void onConnectionChanged(boolean online);
    }

    private final ConnectivityManager connectivityManager;
    // network callbacks come on a background thread, the UI needs the main thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback callback;

    public NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void start(final Listener listener) {
        listener.onConnectionChanged(isOnline());
        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                mainHandler.post(() -> listener.onConnectionChanged(true));
            }

            @Override
            public void onLost(@NonNull Network network) {
                // maybe another network is still there (wifi -> mobile data)
                mainHandler.post(() -> listener.onConnectionChanged(isOnline()));
            }
        };
        connectivityManager.registerDefaultNetworkCallback(callback);
    }

    public void stop() {
        if (callback != null) {
            connectivityManager.unregisterNetworkCallback(callback);
            callback = null;
        }
        mainHandler.removeCallbacksAndMessages(null);
    }

    public boolean isOnline() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
