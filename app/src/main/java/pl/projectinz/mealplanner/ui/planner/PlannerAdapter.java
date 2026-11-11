package pl.projectinz.mealplanner.ui.planner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.logic.DateUtils;
import pl.projectinz.mealplanner.model.PlannedMeal;

// shows one week in the planner. two kinds of rows:
// a day header ("Monday, 16 Nov" with a + button) and the meals of that day
public class PlannerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onAddMealClicked(String date);

        void onMealClicked(PlannedMeal meal);

        void onDeleteMealClicked(PlannedMeal meal);
    }

    private static final int TYPE_DAY = 0;
    private static final int TYPE_MEAL = 1;

    // one row: a day header (meal == null) or a meal
    private static class Row {
        final String date;
        final PlannedMeal meal;
        final boolean dayIsEmpty;

        Row(String date, PlannedMeal meal, boolean dayIsEmpty) {
            this.date = date;
            this.meal = meal;
            this.dayIsEmpty = dayIsEmpty;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public PlannerAdapter(Listener listener) {
        this.listener = listener;
    }

    // builds the rows for 7 days from weekStart
    @SuppressLint("NotifyDataSetChanged")
    public void setWeek(String weekStart, List<PlannedMeal> meals) {
        rows.clear();
        for (int day = 0; day < 7; day++) {
            String date = DateUtils.addDays(weekStart, day);
            List<PlannedMeal> mealsOfDay = new ArrayList<>();
            for (PlannedMeal meal : meals) {
                if (date.equals(meal.getDate())) {
                    mealsOfDay.add(meal);
                }
            }
            rows.add(new Row(date, null, mealsOfDay.isEmpty()));
            for (PlannedMeal meal : mealsOfDay) {
                rows.add(new Row(date, meal, false));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).meal == null ? TYPE_DAY : TYPE_MEAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DAY) {
            return new DayViewHolder(inflater.inflate(R.layout.item_planner_day, parent, false));
        }
        return new MealViewHolder(inflater.inflate(R.layout.item_planned_meal, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof DayViewHolder) {
            ((DayViewHolder) holder).bind(row);
        } else {
            ((MealViewHolder) holder).bind(row.meal);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDay;
        private final TextView textNoMeals;
        private final View buttonAddMeal;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.textDay);
            textNoMeals = itemView.findViewById(R.id.textNoMeals);
            buttonAddMeal = itemView.findViewById(R.id.buttonAddMeal);
        }

        void bind(final Row row) {
            Context context = itemView.getContext();
            Locale locale = context.getResources().getConfiguration().getLocales().get(0);
            String text = DateUtils.formatForDisplay(row.date, "EEEE, d MMM", locale);
            boolean isToday = row.date.equals(DateUtils.today());
            if (isToday) {
                text = text + " • " + context.getString(R.string.planner_today);
            }
            textDay.setText(text);
            // today is shown in the primary colour
            textDay.setTextColor(MaterialColors.getColor(textDay,
                    isToday ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurface));
            textNoMeals.setVisibility(row.dayIsEmpty ? View.VISIBLE : View.GONE);
            buttonAddMeal.setOnClickListener(v -> listener.onAddMealClicked(row.date));
        }
    }

    class MealViewHolder extends RecyclerView.ViewHolder {
        private final TextView textRecipeName;
        private final TextView textPortions;
        private final View buttonDelete;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            textRecipeName = itemView.findViewById(R.id.textRecipeName);
            textPortions = itemView.findViewById(R.id.textPortions);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteMeal);
        }

        void bind(final PlannedMeal meal) {
            textRecipeName.setText(meal.getRecipeName());
            textPortions.setText(itemView.getContext().getResources().getQuantityString(
                    R.plurals.portions_count, meal.getPortions(), meal.getPortions()));
            itemView.setOnClickListener(v -> listener.onMealClicked(meal));
            buttonDelete.setOnClickListener(v -> listener.onDeleteMealClicked(meal));
        }
    }
}
