package pl.projectinz.mealplanner.ui.planner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Locale;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.data.MealPlanRepository;
import pl.projectinz.mealplanner.logic.DateUtils;
import pl.projectinz.mealplanner.model.PlannedMeal;

// Planner tab: meals of one week (Monday - Sunday)
// you can change the week, pick a date, add, edit and delete meals
public class PlannerFragment extends Fragment implements PlannerAdapter.Listener {

    private static final String STATE_WEEK_START = "weekStart";
    private static final String TAG_DATE_PICKER = "plannerDatePicker";

    private final MealPlanRepository mealPlanRepository = new MealPlanRepository();

    // Monday of the week on the screen
    private String weekStart;
    private ListenerRegistration mealsListener;

    private PlannerAdapter adapter;
    private MaterialButton buttonWeek;
    private View progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_planner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        weekStart = savedInstanceState != null
                ? savedInstanceState.getString(STATE_WEEK_START)
                : DateUtils.startOfWeek(DateUtils.today());

        buttonWeek = view.findViewById(R.id.buttonWeek);
        progress = view.findViewById(R.id.progress);

        adapter = new PlannerAdapter(this);
        RecyclerView recycler = view.findViewById(R.id.recyclerPlanner);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        view.findViewById(R.id.buttonPreviousWeek).setOnClickListener(v -> showWeek(DateUtils.addDays(weekStart, -7)));
        view.findViewById(R.id.buttonNextWeek).setOnClickListener(v -> showWeek(DateUtils.addDays(weekStart, 7)));
        view.findViewById(R.id.buttonToday).setOnClickListener(v -> showWeek(DateUtils.startOfWeek(DateUtils.today())));
        buttonWeek.setOnClickListener(v -> openDatePicker());

        // after rotation the date picker is created again - connect it again
        @SuppressWarnings("unchecked")
        MaterialDatePicker<Long> picker =
                (MaterialDatePicker<Long>) getChildFragmentManager().findFragmentByTag(TAG_DATE_PICKER);
        if (picker != null) {
            picker.addOnPositiveButtonClickListener(this::onDatePicked);
        }

        updateWeekTitle();
    }

    @Override
    public void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopListening();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_WEEK_START, weekStart);
    }

    private void showWeek(String newWeekStart) {
        weekStart = newWeekStart;
        updateWeekTitle();
        stopListening();
        startListening();
    }

    private void startListening() {
        progress.setVisibility(View.VISIBLE);
        String weekEnd = DateUtils.addDays(weekStart, 6);
        mealsListener = mealPlanRepository.listenToMealsBetween(weekStart, weekEnd,
                new DataCallback<List<PlannedMeal>>() {
                    @Override
                    public void onSuccess(List<PlannedMeal> meals) {
                        progress.setVisibility(View.GONE);
                        adapter.setWeek(weekStart, meals);
                    }

                    @Override
                    public void onError(Exception e) {
                        progress.setVisibility(View.GONE);
                        showError();
                    }
                });
    }

    private void stopListening() {
        if (mealsListener != null) {
            mealsListener.remove();
            mealsListener = null;
        }
    }

    // e.g. "16 Nov - 22 Nov 2026"
    private void updateWeekTitle() {
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        String weekEnd = DateUtils.addDays(weekStart, 6);
        buttonWeek.setText(getString(R.string.planner_week_range,
                DateUtils.formatForDisplay(weekStart, "d MMM", locale),
                DateUtils.formatForDisplay(weekEnd, "d MMM yyyy", locale)));
    }

    private void openDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.planner_pick_date)
                .setSelection(DateUtils.toUtcMillis(weekStart))
                .build();
        picker.addOnPositiveButtonClickListener(this::onDatePicked);
        picker.show(getChildFragmentManager(), TAG_DATE_PICKER);
    }

    // the picker gives the day as UTC millis
    private void onDatePicked(Long utcMillis) {
        showWeek(DateUtils.startOfWeek(DateUtils.fromUtcMillis(utcMillis)));
    }

    @Override
    public void onAddMealClicked(String date) {
        Intent intent = new Intent(requireContext(), AddEditPlannedMealActivity.class);
        intent.putExtra(AddEditPlannedMealActivity.EXTRA_DATE, date);
        startActivity(intent);
    }

    @Override
    public void onMealClicked(PlannedMeal meal) {
        Intent intent = new Intent(requireContext(), AddEditPlannedMealActivity.class);
        intent.putExtra(AddEditPlannedMealActivity.EXTRA_MEAL_ID, meal.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteMealClicked(PlannedMeal meal) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_meal_title)
                .setMessage(getString(R.string.delete_meal_message, meal.getRecipeName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        mealPlanRepository.deleteMeal(meal.getId(), new DataCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                            }

                            @Override
                            public void onError(Exception e) {
                                showError();
                            }
                        }))
                .show();
    }

    private void showError() {
        if (getView() != null) {
            Snackbar.make(getView(), R.string.error_generic, Snackbar.LENGTH_LONG).show();
        }
    }
}
