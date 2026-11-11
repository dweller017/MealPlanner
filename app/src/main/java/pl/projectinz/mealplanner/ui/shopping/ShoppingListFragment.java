package pl.projectinz.mealplanner.ui.shopping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.data.DataCallback;
import pl.projectinz.mealplanner.data.ShoppingListGenerationService;
import pl.projectinz.mealplanner.data.ShoppingListRepository;
import pl.projectinz.mealplanner.logic.DateUtils;
import pl.projectinz.mealplanner.logic.ShoppingListOrganizer;
import pl.projectinz.mealplanner.model.ShoppingListItem;

// Shopping tab
// - generate from plan: choose dates, the list is made from the planned meals
// - products can be added, edited, deleted and marked as purchased
// - clear purchased removes the purchased ones
public class ShoppingListFragment extends Fragment implements ShoppingListAdapter.Listener {

    private static final String TAG_RANGE_PICKER = "shoppingRangePicker";

    private final ShoppingListRepository shoppingListRepository = new ShoppingListRepository();
    private final ShoppingListGenerationService generationService = new ShoppingListGenerationService();
    private final ShoppingListOrganizer organizer = new ShoppingListOrganizer();

    private List<ShoppingListItem> currentItems = new ArrayList<>();
    private ListenerRegistration itemsListener;

    private ShoppingListAdapter adapter;
    private MaterialButton buttonGenerate;
    private View progressGenerate;
    private View progress;
    private TextView textEmpty;
    private TextView textRange;
    // true while generating or refreshing, so it doesn't run twice
    private boolean generating;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shopping_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        buttonGenerate = view.findViewById(R.id.buttonGenerate);
        progressGenerate = view.findViewById(R.id.progressGenerate);
        progress = view.findViewById(R.id.progress);
        textEmpty = view.findViewById(R.id.textEmpty);
        textRange = view.findViewById(R.id.textRange);

        adapter = new ShoppingListAdapter(this);
        RecyclerView recycler = view.findViewById(R.id.recyclerShopping);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        buttonGenerate.setOnClickListener(v -> openRangePicker());
        view.findViewById(R.id.buttonClearPurchased).setOnClickListener(v -> confirmClearPurchased());
        view.findViewById(R.id.fabAddItem).setOnClickListener(v ->
                ShoppingItemDialog.show(requireContext(), null, item ->
                        shoppingListRepository.addItem(item, errorCallback())));

        // after rotation the picker is created again - connect it again
        @SuppressWarnings("unchecked")
        MaterialDatePicker<Pair<Long, Long>> picker =
                (MaterialDatePicker<Pair<Long, Long>>) getChildFragmentManager().findFragmentByTag(TAG_RANGE_PICKER);
        if (picker != null) {
            picker.addOnPositiveButtonClickListener(this::onRangePicked);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        itemsListener = shoppingListRepository.listenToItems(new DataCallback<List<ShoppingListItem>>() {
            @Override
            public void onSuccess(List<ShoppingListItem> items) {
                currentItems = items;
                progress.setVisibility(View.GONE);
                adapter.setGroups(organizer.group(items));
                textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                progress.setVisibility(View.GONE);
                showMessage(getString(R.string.error_generic));
            }
        });
        refreshFromPlan();
    }

    // calculates the generated products again for the saved dates,
    // so changes in the plan (e.g. portions) show up on the list
    private void refreshFromPlan() {
        if (generating) {
            return;
        }
        setGenerating(true);
        generationService.refresh(new DataCallback<ShoppingListGenerationService.Summary>() {
            @Override
            public void onSuccess(ShoppingListGenerationService.Summary summary) {
                setGenerating(false);
                if (!isAdded() || summary == null) {
                    return;
                }
                showRange(summary.getFromDate(), summary.getToDate());
                if (summary.isChanged()) {
                    showMessage(getString(R.string.shopping_updated_from_plan));
                }
            }

            @Override
            public void onError(Exception e) {
                setGenerating(false);
            }
        });
    }

    // e.g. "List for 9 Nov - 15 Nov ..."
    private void showRange(String fromDate, String toDate) {
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        textRange.setText(getString(R.string.shopping_range_info,
                DateUtils.formatForDisplay(fromDate, "d MMM", locale),
                DateUtils.formatForDisplay(toDate, "d MMM", locale)));
        textRange.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (itemsListener != null) {
            itemsListener.remove();
            itemsListener = null;
        }
    }

    // opens the date range picker with this week selected
    private void openRangePicker() {
        String weekStart = DateUtils.startOfWeek(DateUtils.today());
        String weekEnd = DateUtils.addDays(weekStart, 6);
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.shopping_pick_range)
                .setSelection(new Pair<>(DateUtils.toUtcMillis(weekStart), DateUtils.toUtcMillis(weekEnd)))
                .build();
        picker.addOnPositiveButtonClickListener(this::onRangePicked);
        picker.show(getChildFragmentManager(), TAG_RANGE_PICKER);
    }

    private void onRangePicked(Pair<Long, Long> range) {
        if (range == null || range.first == null || range.second == null) {
            return;
        }
        // the picker gives UTC millis
        generate(DateUtils.fromUtcMillis(range.first), DateUtils.fromUtcMillis(range.second));
    }

    private void generate(final String fromDate, final String toDate) {
        if (generating) {
            return;
        }
        setGenerating(true);
        generationService.generate(fromDate, toDate, new DataCallback<ShoppingListGenerationService.Summary>() {
            @Override
            public void onSuccess(ShoppingListGenerationService.Summary summary) {
                setGenerating(false);
                if (!isAdded()) {
                    return;
                }
                if (summary.getMealCount() == 0) {
                    Locale locale = getResources().getConfiguration().getLocales().get(0);
                    showMessage(getString(R.string.shopping_no_meals_in_range,
                            DateUtils.formatForDisplay(fromDate, "d MMM", locale),
                            DateUtils.formatForDisplay(toDate, "d MMM", locale)));
                    return;
                }
                showRange(fromDate, toDate);
                String message = getResources().getQuantityString(R.plurals.shopping_generated_products,
                        summary.getProductCount(), summary.getProductCount())
                        + " " + getResources().getQuantityString(R.plurals.shopping_generated_meals,
                        summary.getMealCount(), summary.getMealCount());
                if (summary.getSkippedMealCount() > 0) {
                    message += "\n" + getResources().getQuantityString(R.plurals.shopping_skipped_meals,
                            summary.getSkippedMealCount(), summary.getSkippedMealCount());
                }
                showMessage(message);
            }

            @Override
            public void onError(Exception e) {
                setGenerating(false);
                if (isAdded()) {
                    showMessage(getString(R.string.error_generic));
                }
            }
        });
    }

    private void setGenerating(boolean running) {
        generating = running;
        if (getView() == null) {
            return;
        }
        progressGenerate.setVisibility(running ? View.VISIBLE : View.INVISIBLE);
        buttonGenerate.setEnabled(!running);
    }

    private void confirmClearPurchased() {
        int purchasedCount = 0;
        for (ShoppingListItem item : currentItems) {
            if (item.isPurchased()) {
                purchasedCount++;
            }
        }
        if (purchasedCount == 0) {
            showMessage(getString(R.string.shopping_nothing_purchased));
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_clear_purchased)
                .setMessage(getResources().getQuantityString(R.plurals.shopping_clear_purchased_message,
                        purchasedCount, purchasedCount))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        shoppingListRepository.deletePurchased(currentItems, errorCallback()))
                .show();
    }

    @Override
    public void onPurchasedChanged(ShoppingListItem item, boolean purchased) {
        shoppingListRepository.setPurchased(item.getId(), purchased, errorCallback());
    }

    @Override
    public void onItemClicked(ShoppingListItem item) {
        ShoppingItemDialog.show(requireContext(), item, edited ->
                shoppingListRepository.updateItem(edited, errorCallback()));
    }

    @Override
    public void onDeleteClicked(ShoppingListItem item) {
        shoppingListRepository.deleteItem(item.getId(), errorCallback());
        // simple undo: add the same product again
        if (getView() != null) {
            Snackbar.make(getView(), getString(R.string.shopping_item_deleted, item.getName()), Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo, v -> shoppingListRepository.addItem(item, errorCallback()))
                    .show();
        }
    }

    private DataCallback<Void> errorCallback() {
        return new DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    showMessage(getString(R.string.error_save_failed));
                }
            }
        };
    }

    private void showMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
