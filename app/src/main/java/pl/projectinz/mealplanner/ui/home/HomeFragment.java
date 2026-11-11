package pl.projectinz.mealplanner.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.AuthRepository;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.data.MealPlanRepository;
import pl.projectinz.mealplanner.logic.DateUtils;
import pl.projectinz.mealplanner.model.PlannedMeal;
import pl.projectinz.mealplanner.ui.main.MainActivity;
import pl.projectinz.mealplanner.ui.recipes.RecipeDetailsActivity;

// Home tab: greeting, today's meals and buttons to the other tabs
public class HomeFragment extends Fragment {

    private final MealPlanRepository mealPlanRepository = new MealPlanRepository();
    private ListenerRegistration todayListener;

    private LinearLayout containerTodayMeals;
    private TextView textNoMealsToday;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AuthRepository authRepository = new AuthRepository();
        String name = authRepository.getCurrentUserName();
        if (name == null || name.isEmpty()) {
            name = authRepository.getCurrentUserEmail();
        }
        TextView textWelcome = view.findViewById(R.id.textWelcome);
        textWelcome.setText(getString(R.string.home_welcome, name));

        containerTodayMeals = view.findViewById(R.id.containerTodayMeals);
        textNoMealsToday = view.findViewById(R.id.textNoMealsToday);

        MainActivity activity = (MainActivity) requireActivity();
        view.findViewById(R.id.buttonRecipes).setOnClickListener(v -> activity.selectSection(R.id.nav_recipes));
        view.findViewById(R.id.buttonPlanner).setOnClickListener(v -> activity.selectSection(R.id.nav_planner));
        view.findViewById(R.id.buttonShopping).setOnClickListener(v -> activity.selectSection(R.id.nav_shopping));
    }

    @Override
    public void onStart() {
        super.onStart();
        String today = DateUtils.today();
        todayListener = mealPlanRepository.listenToMealsBetween(today, today, new DataCallback<List<PlannedMeal>>() {
            @Override
            public void onSuccess(List<PlannedMeal> meals) {
                showTodayMeals(meals);
            }

            @Override
            public void onError(Exception e) {
                textNoMealsToday.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (todayListener != null) {
            todayListener.remove();
            todayListener = null;
        }
    }

    private void showTodayMeals(List<PlannedMeal> meals) {
        containerTodayMeals.removeAllViews();
        textNoMealsToday.setVisibility(meals.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (final PlannedMeal meal : meals) {
            View row = inflater.inflate(R.layout.item_planned_meal, containerTodayMeals, false);
            ((TextView) row.findViewById(R.id.textRecipeName)).setText(meal.getRecipeName());
            ((TextView) row.findViewById(R.id.textPortions)).setText(getResources().getQuantityString(
                    R.plurals.portions_count, meal.getPortions(), meal.getPortions()));
            row.findViewById(R.id.buttonDeleteMeal).setVisibility(View.GONE);
            // open the recipe with quantities for the planned portions
            row.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), RecipeDetailsActivity.class);
                intent.putExtra(RecipeDetailsActivity.EXTRA_RECIPE_ID, meal.getRecipeId());
                intent.putExtra(RecipeDetailsActivity.EXTRA_PORTIONS, meal.getPortions());
                startActivity(intent);
            });
            containerTodayMeals.addView(row);
        }
    }
}
