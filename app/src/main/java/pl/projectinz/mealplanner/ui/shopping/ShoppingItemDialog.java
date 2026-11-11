package pl.projectinz.mealplanner.ui.shopping;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.logic.InputValidator;
import pl.projectinz.mealplanner.logic.QuantityFormatter;
import pl.projectinz.mealplanner.model.ItemSource;
import pl.projectinz.mealplanner.model.MeasurementUnit;
import pl.projectinz.mealplanner.model.ShoppingListItem;
import pl.projectinz.mealplanner.ui.common.UnitLabels;

// dialog for adding or editing a product
// quantity and unit are optional (e.g. "Bread"), but a quantity needs a unit
public class ShoppingItemDialog {

    public interface Listener {
        void onItemSaved(ShoppingListItem item);
    }

    // position 0 in the unit dropdown = no unit
    private static final int NO_UNIT = 0;

    private ShoppingItemDialog() {
    }

    // existing = product to edit, or null for a new one
    public static void show(Context context, final ShoppingListItem existing, final Listener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_shopping_item, null);
        final TextInputLayout layoutName = view.findViewById(R.id.layoutName);
        final TextInputLayout layoutQuantity = view.findViewById(R.id.layoutQuantity);
        final TextInputLayout layoutUnit = view.findViewById(R.id.layoutUnit);
        final TextInputEditText editName = view.findViewById(R.id.editName);
        final TextInputEditText editQuantity = view.findViewById(R.id.editQuantity);
        final MaterialAutoCompleteTextView dropdownUnit = view.findViewById(R.id.dropdownUnit);
        final MaterialAutoCompleteTextView editCategory = view.findViewById(R.id.editCategory);

        // unit options: "-" (no unit), then g, kg, ml, l, pcs
        String[] unitLabels = UnitLabels.getAllLabels(context);
        final String[] options = new String[unitLabels.length + 1];
        options[0] = context.getString(R.string.unit_none);
        System.arraycopy(unitLabels, 0, options, 1, unitLabels.length);
        dropdownUnit.setSimpleItems(options);
        final int[] selectedUnit = {NO_UNIT};
        dropdownUnit.setOnItemClickListener((parent, itemView, position, id) -> {
            selectedUnit[0] = position;
            layoutUnit.setError(null);
        });
        dropdownUnit.setText(options[NO_UNIT], false);

        editCategory.setSimpleItems(context.getResources().getStringArray(R.array.shopping_categories));

        if (existing != null) {
            editName.setText(existing.getName());
            if (existing.getQuantity() > 0) {
                Locale locale = context.getResources().getConfiguration().getLocales().get(0);
                editQuantity.setText(new QuantityFormatter().format(existing.getQuantity(), locale));
            }
            if (existing.getUnit() != null) {
                selectedUnit[0] = existing.getUnit().ordinal() + 1;
                dropdownUnit.setText(options[selectedUnit[0]], false);
            }
            editCategory.setText(existing.getCategory(), false);
            if (existing.getSource() == ItemSource.GENERATED) {
                view.findViewById(R.id.textGeneratedHint).setVisibility(View.VISIBLE);
            }
        }

        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(existing == null ? R.string.title_add_product : R.string.title_edit_product)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .show();

        // own click listener so the dialog closes only when the input is ok
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            InputValidator validator = new InputValidator();
            layoutName.setError(null);
            layoutQuantity.setError(null);
            layoutUnit.setError(null);

            String name = textOf(editName).trim();
            String quantityText = textOf(editQuantity).trim();
            boolean hasUnit = selectedUnit[0] != NO_UNIT;
            boolean valid = true;

            if (!validator.isNotBlank(name)) {
                layoutName.setError(context.getString(R.string.error_required));
                valid = false;
            }
            Double quantity = null;
            if (!quantityText.isEmpty()) {
                quantity = validator.parseQuantity(quantityText);
                if (quantity == null) {
                    layoutQuantity.setError(context.getString(R.string.error_quantity_invalid, (int) InputValidator.MAX_QUANTITY));
                    valid = false;
                } else if (!hasUnit) {
                    layoutUnit.setError(context.getString(R.string.error_unit_required));
                    valid = false;
                }
            } else if (hasUnit) {
                layoutQuantity.setError(context.getString(R.string.error_quantity_invalid, (int) InputValidator.MAX_QUANTITY));
                valid = false;
            }
            if (!valid) {
                return;
            }

            ShoppingListItem item = existing != null
                    ? existing
                    : new ShoppingListItem(name, 0, null, ItemSource.MANUAL);
            item.setName(name);
            item.setQuantity(quantity != null ? quantity : 0);
            item.setUnit(hasUnit ? MeasurementUnit.values()[selectedUnit[0] - 1] : null);
            String category = textOf(editCategory).trim();
            item.setCategory(category.isEmpty() ? null : category);

            listener.onItemSaved(item);
            dialog.dismiss();
        });
    }

    private static String textOf(TextView view) {
        return view.getText() != null ? view.getText().toString() : "";
    }
}
