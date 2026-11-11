package pl.projectinz.mealplanner.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.AuthRepository;
import pl.projectinz.mealplanner.ui.auth.LoginActivity;
import pl.projectinz.mealplanner.ui.common.EdgeToEdgeHelper;
import pl.projectinz.mealplanner.ui.common.NetworkMonitor;
import pl.projectinz.mealplanner.ui.home.HomeFragment;
import pl.projectinz.mealplanner.ui.planner.PlannerFragment;
import pl.projectinz.mealplanner.ui.recipes.RecipesFragment;
import pl.projectinz.mealplanner.ui.settings.SettingsActivity;
import pl.projectinz.mealplanner.ui.shopping.ShoppingListFragment;

// main screen with bottom navigation
// each tab (Home, Recipes, Planner, Shopping) is a Fragment put into fragmentContainer
public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private View textOffline;
    private NetworkMonitor networkMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // only for logged in users
        if (!new AuthRepository().isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdgeHelper.enable(this);
        setContentView(R.layout.activity_main);
        EdgeToEdgeHelper.applyPadding(findViewById(R.id.root));

        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main_toolbar_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        textOffline = findViewById(R.id.textOffline);
        networkMonitor = new NetworkMonitor(this);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            showSection(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            // first start - show Home
            showSection(R.id.nav_home);
        } else {
            // after rotation the FragmentManager brings the fragment back by itself
            updateTitle(bottomNavigation.getSelectedItemId());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (networkMonitor != null) {
            networkMonitor.start(online -> textOffline.setVisibility(online ? View.GONE : View.VISIBLE));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkMonitor != null) {
            networkMonitor.stop();
        }
    }

    // switch the tab from code, e.g. from a button on Home
    public void selectSection(int menuItemId) {
        bottomNavigation.setSelectedItemId(menuItemId);
    }

    private void showSection(int menuItemId) {
        Fragment fragment;
        if (menuItemId == R.id.nav_recipes) {
            fragment = new RecipesFragment();
        } else if (menuItemId == R.id.nav_planner) {
            fragment = new PlannerFragment();
        } else if (menuItemId == R.id.nav_shopping) {
            fragment = new ShoppingListFragment();
        } else {
            fragment = new HomeFragment();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        updateTitle(menuItemId);
    }

    private void updateTitle(int menuItemId) {
        if (menuItemId == R.id.nav_recipes) {
            toolbar.setTitle(R.string.nav_recipes);
        } else if (menuItemId == R.id.nav_planner) {
            toolbar.setTitle(R.string.nav_planner);
        } else if (menuItemId == R.id.nav_shopping) {
            toolbar.setTitle(R.string.nav_shopping);
        } else {
            toolbar.setTitle(R.string.app_name);
        }
    }
}
