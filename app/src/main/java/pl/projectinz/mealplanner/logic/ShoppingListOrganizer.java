package pl.projectinz.mealplanner.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import pl.projectinz.mealplanner.model.ItemSource;
import pl.projectinz.mealplanner.model.ShoppingListItem;

// prepares the shopping list for showing it:
// - groups products by category (no category = last group)
// - products to buy first, purchased at the end, sorted by name
//
// it also remembers categories, so after generating again
// a product gets the category the user gave it before
public class ShoppingListOrganizer {

    // the generator rounds to 3 decimals, so a smaller difference means equal
    private static final double QUANTITY_TOLERANCE = 0.0005;

    // one category and its products, category == null means no category
    public static class Group {
        private final String category;
        private final List<ShoppingListItem> items;

        public Group(String category, List<ShoppingListItem> items) {
            this.category = category;
            this.items = items;
        }

        public String getCategory() {
            return category;
        }

        public List<ShoppingListItem> getItems() {
            return items;
        }
    }

    public List<Group> group(List<ShoppingListItem> items) {
        // TreeMap keeps the categories sorted (big/small letters don't matter)
        Map<String, List<ShoppingListItem>> byCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<ShoppingListItem> withoutCategory = new ArrayList<>();

        if (items != null) {
            for (ShoppingListItem item : items) {
                String category = item.getCategory() != null ? item.getCategory().trim() : "";
                if (category.isEmpty()) {
                    withoutCategory.add(item);
                } else {
                    List<ShoppingListItem> list = byCategory.get(category);
                    if (list == null) {
                        list = new ArrayList<>();
                        byCategory.put(category, list);
                    }
                    list.add(item);
                }
            }
        }

        List<Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<ShoppingListItem>> entry : byCategory.entrySet()) {
            sortItems(entry.getValue());
            groups.add(new Group(entry.getKey(), entry.getValue()));
        }
        if (!withoutCategory.isEmpty()) {
            sortItems(withoutCategory);
            groups.add(new Group(null, withoutCategory));
        }
        return groups;
    }

    // to buy first, then purchased, sorted by name
    private void sortItems(List<ShoppingListItem> items) {
        Collections.sort(items, new Comparator<ShoppingListItem>() {
            @Override
            public int compare(ShoppingListItem a, ShoppingListItem b) {
                if (a.isPurchased() != b.isPurchased()) {
                    return a.isPurchased() ? 1 : -1;
                }
                return String.valueOf(a.getName()).compareToIgnoreCase(String.valueOf(b.getName()));
            }
        });
    }

    // new products get the category of an old product with the same name
    // e.g. user put "Milk" in "Dairy" -> after generating again it's in "Dairy" too
    public void copyKnownCategories(List<ShoppingListItem> newItems, List<ShoppingListItem> existingItems) {
        if (newItems == null || existingItems == null) {
            return;
        }
        Map<String, String> categoryByName = new HashMap<>();
        for (ShoppingListItem existing : existingItems) {
            String category = existing.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                categoryByName.put(normalize(existing.getName()), category.trim());
            }
        }
        for (ShoppingListItem item : newItems) {
            if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                String known = categoryByName.get(normalize(item.getName()));
                if (known != null) {
                    item.setCategory(known);
                }
            }
        }
    }

    // checks if the new list has the same products (name, quantity, unit)
    // as the GENERATED ones we have now - then we don't need to save anything
    public boolean hasSameGeneratedContent(List<ShoppingListItem> currentItems, List<ShoppingListItem> newItems) {
        Map<String, Double> current = new HashMap<>();
        if (currentItems != null) {
            for (ShoppingListItem item : currentItems) {
                if (item.getSource() == ItemSource.GENERATED) {
                    current.put(key(item), item.getQuantity());
                }
            }
        }
        int newCount = newItems != null ? newItems.size() : 0;
        if (current.size() != newCount) {
            return false;
        }
        for (ShoppingListItem item : newItems) {
            Double quantity = current.get(key(item));
            if (quantity == null || Math.abs(quantity - item.getQuantity()) > QUANTITY_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    // keeps the "purchased" tick after recalculating, but only if the quantity
    // is still the same. if the plan needs more now, it has to be bought again
    public void copyPurchasedState(List<ShoppingListItem> newItems, List<ShoppingListItem> currentItems) {
        if (newItems == null || currentItems == null) {
            return;
        }
        Map<String, ShoppingListItem> purchased = new HashMap<>();
        for (ShoppingListItem item : currentItems) {
            if (item.getSource() == ItemSource.GENERATED && item.isPurchased()) {
                purchased.put(key(item), item);
            }
        }
        for (ShoppingListItem item : newItems) {
            ShoppingListItem old = purchased.get(key(item));
            if (old != null && Math.abs(old.getQuantity() - item.getQuantity()) <= QUANTITY_TOLERANCE) {
                item.setPurchased(true);
            }
        }
    }

    // key of a product: name + unit, e.g. "chicken|G"
    private static String key(ShoppingListItem item) {
        return normalize(item.getName()) + "|" + item.getUnit();
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
