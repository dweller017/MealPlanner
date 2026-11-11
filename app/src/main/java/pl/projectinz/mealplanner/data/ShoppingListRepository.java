package pl.projectinz.mealplanner.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import pl.projectinz.mealplanner.model.ItemSource;
import pl.projectinz.mealplanner.model.ShoppingListItem;

// the shopping list of the user (only one list): users/{uid}/shoppingItems
public class ShoppingListRepository {

    // Firestore allows max 500 operations in one batch
    private static final int MAX_BATCH_OPERATIONS = 450;

    private final CollectionReference items;

    public ShoppingListRepository() {
        String uid = FirebaseAuth.getInstance().getUid();
        this.items = FirestorePaths.shoppingItems(uid);
    }

    // listens to all products, called again after every change
    public ListenerRegistration listenToItems(final DataCallback<List<ShoppingListItem>> callback) {
        return items.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            callback.onSuccess(toList(snapshot));
        });
    }

    // reads all products once
    public void getAllItems(final DataCallback<List<ShoppingListItem>> callback) {
        items.get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(toList(snapshot)))
                .addOnFailureListener(callback::onError);
    }

    public void addItem(ShoppingListItem item, final DataCallback<Void> callback) {
        items.document().set(item)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void updateItem(ShoppingListItem item, final DataCallback<Void> callback) {
        items.document(item.getId()).set(item)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void setPurchased(String itemId, boolean purchased, final DataCallback<Void> callback) {
        items.document(itemId).update("purchased", purchased)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void deleteItem(String itemId, final DataCallback<Void> callback) {
        items.document(itemId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // deletes all products that are marked as purchased
    public void deletePurchased(List<ShoppingListItem> currentItems, final DataCallback<Void> callback) {
        BatchWriter writer = new BatchWriter(callback);
        for (ShoppingListItem item : currentItems) {
            if (item.isPurchased()) {
                writer.delete(item.getId());
            }
        }
        writer.commit();
    }

    // deletes all GENERATED products and adds the new ones
    // MANUAL products are not touched
    public void replaceGeneratedItems(List<ShoppingListItem> currentItems, List<ShoppingListItem> newItems,
                                      final DataCallback<Void> callback) {
        BatchWriter writer = new BatchWriter(callback);
        for (ShoppingListItem item : currentItems) {
            if (item.getSource() == ItemSource.GENERATED) {
                writer.delete(item.getId());
            }
        }
        for (ShoppingListItem item : newItems) {
            writer.add(item);
        }
        writer.commit();
    }

    private static List<ShoppingListItem> toList(QuerySnapshot snapshot) {
        List<ShoppingListItem> result = new ArrayList<>();
        if (snapshot != null) {
            for (QueryDocumentSnapshot document : snapshot) {
                result.add(document.toObject(ShoppingListItem.class));
            }
        }
        return result;
    }

    // helper that puts many writes into batches
    // a batch is saved all at once, if it gets full a new one is started
    private class BatchWriter {
        private final DataCallback<Void> callback;
        private final List<WriteBatch> batches = new ArrayList<>();
        private WriteBatch current;
        private int operationsInCurrent;

        BatchWriter(DataCallback<Void> callback) {
            this.callback = callback;
        }

        private WriteBatch batch() {
            if (current == null || operationsInCurrent >= MAX_BATCH_OPERATIONS) {
                current = FirebaseFirestore.getInstance().batch();
                batches.add(current);
                operationsInCurrent = 0;
            }
            operationsInCurrent++;
            return current;
        }

        void delete(String itemId) {
            batch().delete(items.document(itemId));
        }

        void add(ShoppingListItem item) {
            batch().set(items.document(), item);
        }

        void commit() {
            for (WriteBatch batch : batches) {
                batch.commit().addOnFailureListener(callback::onError);
            }
            // Firestore applies it locally right away, errors come in onError
            callback.onSuccess(null);
        }
    }
}
