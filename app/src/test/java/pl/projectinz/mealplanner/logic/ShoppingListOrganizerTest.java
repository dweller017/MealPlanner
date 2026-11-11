package pl.projectinz.mealplanner.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pl.projectinz.mealplanner.model.ItemSource;
import pl.projectinz.mealplanner.model.MeasurementUnit;
import pl.projectinz.mealplanner.model.ShoppingListItem;

public class ShoppingListOrganizerTest {

    private final ShoppingListOrganizer organizer = new ShoppingListOrganizer();

    private static ShoppingListItem item(String name, String category, boolean purchased) {
        ShoppingListItem item = new ShoppingListItem(name, 1, MeasurementUnit.PCS, ItemSource.MANUAL);
        item.setCategory(category);
        item.setPurchased(purchased);
        return item;
    }

    @Test
    public void groupsByCategoryAlphabeticallyWithUncategorisedLast() {
        List<ShoppingListOrganizer.Group> groups = organizer.group(Arrays.asList(
                item("Bread", null, false),
                item("Milk", "Dairy", false),
                item("Apple", "Vegetables & fruit", false),
                item("Cheese", "dairy", false)));

        assertEquals(3, groups.size());
        assertEquals("Dairy", groups.get(0).getCategory());
        assertEquals(2, groups.get(0).getItems().size()); // "Dairy" and "dairy" are the same group
        assertEquals("Vegetables & fruit", groups.get(1).getCategory());
        assertNull(groups.get(2).getCategory());
        assertEquals("Bread", groups.get(2).getItems().get(0).getName());
    }

    @Test
    public void purchasedItemsGoToTheEndOfTheirGroup() {
        List<ShoppingListOrganizer.Group> groups = organizer.group(Arrays.asList(
                item("Apple", null, true),
                item("Carrot", null, false),
                item("Banana", null, false)));

        List<ShoppingListItem> items = groups.get(0).getItems();
        assertEquals("Banana", items.get(0).getName());
        assertEquals("Carrot", items.get(1).getName());
        assertEquals("Apple", items.get(2).getName());
    }

    @Test
    public void blankCategoryCountsAsNoCategory() {
        List<ShoppingListOrganizer.Group> groups = organizer.group(Collections.singletonList(item("Salt", "  ", false)));
        assertEquals(1, groups.size());
        assertNull(groups.get(0).getCategory());
    }

    @Test
    public void emptyListGivesNoGroups() {
        assertTrue(organizer.group(new ArrayList<ShoppingListItem>()).isEmpty());
        assertTrue(organizer.group(null).isEmpty());
    }

    @Test
    public void copiesCategoryFromProductWithTheSameName() {
        List<ShoppingListItem> existing = Arrays.asList(item("Milk", "Dairy", false), item("Rice", null, false));
        ShoppingListItem newMilk = item(" milk ", null, false);
        ShoppingListItem newRice = item("Rice", null, false);
        ShoppingListItem newOnion = item("Onion", null, false);

        organizer.copyKnownCategories(Arrays.asList(newMilk, newRice, newOnion), existing);

        assertEquals("Dairy", newMilk.getCategory());
        assertNull(newRice.getCategory());
        assertNull(newOnion.getCategory());
    }

    private static ShoppingListItem generated(String name, double quantity, MeasurementUnit unit, boolean purchased) {
        ShoppingListItem item = new ShoppingListItem(name, quantity, unit, ItemSource.GENERATED);
        item.setPurchased(purchased);
        return item;
    }

    @Test
    public void sameGeneratedContentIsDetected() {
        List<ShoppingListItem> current = Arrays.asList(
                generated("Chicken", 500, MeasurementUnit.G, true),
                generated("Onion", 1, MeasurementUnit.PCS, false),
                item("Bread", null, false)); // manual items are ignored in the comparison
        List<ShoppingListItem> recalculated = Arrays.asList(
                generated("chicken", 500, MeasurementUnit.G, false),
                generated("Onion", 1, MeasurementUnit.PCS, false));

        assertTrue(organizer.hasSameGeneratedContent(current, recalculated));
    }

    @Test
    public void changedPortionsAreDetected() {
        // planned portions changed: chicken 500 g -> 700 g
        List<ShoppingListItem> current = Collections.singletonList(generated("Chicken", 500, MeasurementUnit.G, false));
        List<ShoppingListItem> recalculated = Collections.singletonList(generated("Chicken", 700, MeasurementUnit.G, false));
        assertFalse(organizer.hasSameGeneratedContent(current, recalculated));
    }

    @Test
    public void addedOrRemovedProductsAreDetected() {
        List<ShoppingListItem> current = Collections.singletonList(generated("Chicken", 500, MeasurementUnit.G, false));
        assertFalse(organizer.hasSameGeneratedContent(current, Arrays.asList(
                generated("Chicken", 500, MeasurementUnit.G, false),
                generated("Rice", 100, MeasurementUnit.G, false))));
        assertFalse(organizer.hasSameGeneratedContent(current, new ArrayList<ShoppingListItem>()));
    }

    @Test
    public void purchasedMarkIsKeptOnlyWhenQuantityIsUnchanged() {
        List<ShoppingListItem> current = Arrays.asList(
                generated("Chicken", 500, MeasurementUnit.G, true),
                generated("Rice", 100, MeasurementUnit.G, true),
                generated("Onion", 1, MeasurementUnit.PCS, false));
        ShoppingListItem chicken = generated("Chicken", 700, MeasurementUnit.G, false); // more needed now
        ShoppingListItem rice = generated("Rice", 100, MeasurementUnit.G, false);       // unchanged
        ShoppingListItem onion = generated("Onion", 1, MeasurementUnit.PCS, false);

        organizer.copyPurchasedState(Arrays.asList(chicken, rice, onion), current);

        assertFalse(chicken.isPurchased());
        assertTrue(rice.isPurchased());
        assertFalse(onion.isPurchased());
    }

    @Test
    public void doesNotOverwriteExistingCategory() {
        ShoppingListItem newItem = item("Milk", "Drinks", false);
        organizer.copyKnownCategories(Collections.singletonList(newItem),
                Collections.singletonList(item("Milk", "Dairy", false)));
        assertEquals("Drinks", newItem.getCategory());
    }
}
