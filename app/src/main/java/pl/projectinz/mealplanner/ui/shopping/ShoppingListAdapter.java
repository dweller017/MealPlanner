package pl.projectinz.mealplanner.ui.shopping;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import pl.projectinz.mealplanner.R;
import pl.projectinz.mealplanner.logic.ShoppingListOrganizer;
import pl.projectinz.mealplanner.model.ItemSource;
import pl.projectinz.mealplanner.model.ShoppingListItem;
import pl.projectinz.mealplanner.ui.common.UnitLabels;

// shows the shopping list grouped by category
// rows are category headers (item == null) or products
public class ShoppingListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onPurchasedChanged(ShoppingListItem item, boolean purchased);

        void onItemClicked(ShoppingListItem item);

        void onDeleteClicked(ShoppingListItem item);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private static class Row {
        final String category;
        final ShoppingListItem item;

        Row(String category, ShoppingListItem item) {
            this.category = category;
            this.item = item;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public ShoppingListAdapter(Listener listener) {
        this.listener = listener;
    }

    // shows the groups, headers only if at least one product has a category
    @SuppressLint("NotifyDataSetChanged")
    public void setGroups(List<ShoppingListOrganizer.Group> groups) {
        rows.clear();
        boolean showHeaders = false;
        for (ShoppingListOrganizer.Group group : groups) {
            if (group.getCategory() != null) {
                showHeaders = true;
                break;
            }
        }
        for (ShoppingListOrganizer.Group group : groups) {
            if (showHeaders) {
                rows.add(new Row(group.getCategory(), null));
            }
            for (ShoppingListItem item : group.getItems()) {
                rows.add(new Row(group.getCategory(), item));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).item == null ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_shopping_header, parent, false));
        }
        return new ItemViewHolder(inflater.inflate(R.layout.item_shopping_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(row.category);
        } else {
            ((ItemViewHolder) holder).bind(row.item);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCategory;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.textCategory);
        }

        void bind(String category) {
            textCategory.setText(category != null ? category
                    : itemView.getContext().getString(R.string.shopping_category_other));
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox checkPurchased;
        private final TextView textName;
        private final TextView textDetails;
        private final View buttonDelete;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            checkPurchased = itemView.findViewById(R.id.checkPurchased);
            textName = itemView.findViewById(R.id.textName);
            textDetails = itemView.findViewById(R.id.textDetails);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        void bind(final ShoppingListItem item) {
            Context context = itemView.getContext();
            textName.setText(item.getName());

            // e.g. "500 g - from meal plan" or "added manually"
            String source = context.getString(item.getSource() == ItemSource.GENERATED
                    ? R.string.shopping_source_generated : R.string.shopping_source_manual);
            if (item.getQuantity() > 0 && item.getUnit() != null) {
                textDetails.setText(context.getString(R.string.shopping_item_details,
                        UnitLabels.formatQuantity(context, item.getQuantity(), item.getUnit()), source));
            } else {
                textDetails.setText(source);
            }

            // purchased products are crossed out and faded
            if (item.isPurchased()) {
                textName.setPaintFlags(textName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textName.setPaintFlags(textName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
            itemView.setAlpha(item.isPurchased() ? 0.5f : 1f);

            // remove the old listener first, otherwise recycling a row would call it
            checkPurchased.setOnCheckedChangeListener(null);
            checkPurchased.setChecked(item.isPurchased());
            checkPurchased.setOnCheckedChangeListener((button, checked) ->
                    listener.onPurchasedChanged(item, checked));

            itemView.setOnClickListener(v -> listener.onItemClicked(item));
            buttonDelete.setOnClickListener(v -> listener.onDeleteClicked(item));
        }
    }
}
