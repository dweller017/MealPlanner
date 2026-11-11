# Firestore database structure

All data belongs to a user and is stored under that user's document.
The document ID is the Firebase Authentication UID.

```
users/{uid}                              User
users/{uid}/recipes/{recipeId}           Recipe (ingredients embedded)
users/{uid}/mealPlans/{plannedMealId}    PlannedMeal
users/{uid}/shoppingItems/{itemId}       ShoppingListItem (one active list)
```

## Documents

| Collection | Fields | Java class |
|---|---|---|
| `users` | `email`, `displayName`, `createdAt`, `shoppingListFrom`, `shoppingListTo` (date range of the current shopping list) | `model.User` |
| `recipes` | `name`, `description`, `defaultPortions` (int), `category`, `favourite` (bool), `ingredients` (array of `{name, quantity, unit}`), `createdAt` | `model.Recipe`, `model.Ingredient` |
| `mealPlans` | `recipeId`, `recipeName`, `date` (`"yyyy-MM-dd"`), `portions` (int), `createdAt` | `model.PlannedMeal` |
| `shoppingItems` | `name`, `quantity` (0 = no quantity), `unit` (optional), `category`, `purchased` (bool), `source` (`GENERATED`/`MANUAL`), `createdAt` | `model.ShoppingListItem` |

`unit` is stored as the enum name: `G`, `KG`, `ML`, `L`, `PCS`.
The document ID is not stored as a field. Firestore copies it into the field marked `@DocumentId` when reading.

## Design decisions

1. **Data nested under `users/{uid}`.** A single security rule
   (`request.auth.uid == userId`) guarantees that each user can access only
   their own recipes, meal plans and shopping list. See `firestore.rules`.
2. **Ingredients embedded in the recipe.** Ingredients are always read and
   saved together with their recipe and a recipe has few of them (far below
   the 1 MiB document limit). One read gives the complete recipe, and saving
   a recipe is a single atomic write.
3. **Recipe name copied into the planned meal (denormalisation).** The planner
   can display meals without loading every recipe. When a recipe is renamed,
   the new name is copied to its planned meals in one `WriteBatch`; when a recipe
   is deleted, its planned meals are deleted too. If a planned meal still points
   to a missing recipe, the generator skips it and reports it.
4. **Dates as `"yyyy-MM-dd"` text.** A planned meal belongs to a day, not to a
   moment in time. This format sorts the same way as dates, so
   `whereGreaterThanOrEqualTo("date", from).whereLessThanOrEqualTo("date", to)`
   selects a date range without time-zone problems.
5. **One active shopping list as a flat collection.** Each product is a separate
   document, so ticking a product as purchased updates one small document.
   Regenerating the list deletes only `source == GENERATED` items and keeps
   manually added ones (done in `WriteBatch`es of at most 450 operations).
   The date range of the list is stored in `users/{uid}`, so the list is
   recalculated automatically when the meal plan changes.
6. **Numbers.** Quantities are `double` (decimal values such as 0.5 kg are
   allowed) and portions are `int`.

## Security rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
