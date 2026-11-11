# Testing – Meal Planner

This document describes how the application was tested: the strategy, the automated unit tests
with their results and code coverage, the manual system test cases, and a traceability matrix
that links every requirement of the specification to its implementation and its tests.

---

## 1. Testing strategy

The application is tested on two levels.

| Level | What is tested | Tool | Where |
|---|---|---|---|
| **Unit tests** (automated) | Business logic: portion calculation, unit conversion, shopping-list generation, list organisation, validation, search, dates | JUnit 4 on the JVM | `app/src/test/java/.../logic/` |
| **System tests** (manual) | Complete user scenarios on a device: screens, Firebase Authentication, Firestore, offline mode, languages, themes | Android Emulator (API 37, Google Play image) | Section 4 of this document |

**Why the logic is tested without Android.** All calculations live in the `logic` package, which
has no `android.*` and no Firebase imports. Its tests run in milliseconds on the developer's
computer and need neither an emulator nor a network. This was a deliberate design decision: the
most important part of the project, the shopping-list generation, can be checked automatically
after every change.

**Why the UI and Firebase layers are tested manually.** Automated tests of these layers would need
the Firebase Emulator Suite and Espresso instrumentation tests. For an application of this size,
the layer separation together with manual scenarios gives sufficient confidence at a much lower
cost. The data and UI layers contain no calculations, only calls to Firebase and to the `logic`
classes.

---

## 2. Unit tests

### 2.1 How to run

In Android Studio, right-click `app/src/test/java` and choose **Run 'Tests in …'**. From the command line:

```bash
./gradlew testDebugUnitTest
```

HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`

### 2.2 Results

All **77 tests pass** (0 failures, 0 errors).

| Test class | Tests | Class under test | What is verified |
|---|---:|---|---|
| `ShoppingListGeneratorTest` | 20 | `ShoppingListGenerator` | The specification example, portion recalculation, merging identical ingredients, unit normalisation, edge cases |
| `UnitConverterTest` | 12 | `UnitConverter` | Conversion g↔kg and ml↔l, compatibility, refusing g→pcs, normalisation to kg / l |
| `PortionCalculatorTest` | 10 | `PortionCalculator` | Formula `quantity × selected / default`, decimals, invalid arguments |
| `ShoppingListOrganizerTest` | 10 | `ShoppingListOrganizer` | Grouping by category, ordering, remembering categories, change detection, keeping the "purchased" mark |
| `InputValidatorTest` | 9 | `InputValidator` | Email, password, portions 1–99, quantities with `.` and `,`, maximum values |
| `FormatterAndDateUtilsTest` | 9 | `QuantityFormatter`, `DateUtils` | Decimal separator per language, date conversion, week start, date ranges |
| `RecipeFilterTest` | 7 | `RecipeFilter` | Search by name / category / ingredient, favourites filter, sorting |
| **Total** | **77** | | |

### 2.3 Test cases required by the specification

| Required case | Test(s) |
|---|---|
| Portion calculation: 2 portions, 200 g chicken → 4 portions = 400 g | `PortionCalculatorTest.doublesQuantityWhenPortionsAreDoubled`, `ShoppingListGeneratorTest.recalculatesPortions` |
| Combining identical ingredients: 200 g + 300 g = 500 g | `ShoppingListGeneratorTest.sumsIdenticalIngredients`, `combinesTwoRecipesFromSpecification` |
| Different ingredients stay separate | `ShoppingListGeneratorTest.differentIngredientsStaySeparate` |
| Measurement units: 500 g + 1 kg = 1.5 kg | `ShoppingListGeneratorTest.sumsGramsAndKilograms`, `UnitConverterTest.sumOfGramsAndKilogramsInBaseUnit` |
| Incompatible units are not converted (g vs pcs) | `ShoppingListGeneratorTest.incompatibleUnitsAreNotCombined`, `UnitConverterTest.refusesToConvertGramsToPieces` |
| Different portion counts in several recipes | `ShoppingListGeneratorTest.combinesRecipesWithDifferentPortionCounts` |
| Empty ingredient list | `ShoppingListGeneratorTest.recipeWithoutIngredientsGivesEmptyShoppingList` |
| One planned meal | `ShoppingListGeneratorTest.singlePlannedMeal` |
| Several planned meals | `ShoppingListGeneratorTest.severalPlannedMeals` |
| Decimal quantities | `ShoppingListGeneratorTest.decimalQuantitiesAreSummedAndRounded`, `PortionCalculatorTest.worksWithDecimalQuantities` |

Additional edge cases (beyond the specification): an empty meal list or `null` input, a meal whose
recipe was deleted, a meal with 0 portions, invalid ingredients (empty name, quantity 0, no unit),
names differing only in letter case or spaces, the same recipe planned twice, floating-point rounding
(0.1 kg + 0.2 kg = exactly 300 g).

### 2.4 Code coverage

Coverage of the unit tests, measured with the JaCoCo integration of the Android Gradle Plugin:

```bash
./gradlew createDebugUnitTestCoverageReport
```

HTML report: `app/build/reports/coverage/test/debug/index.html`

| Package | Line coverage | Branch coverage | Method coverage |
|---|---:|---:|---:|
| `logic` (business logic) | **95.3 %** (244 / 256) | **88.5 %** (169 / 191) | **98.5 %** (64 / 65) |

| Class | Lines | Branches |
|---|---:|---:|
| `PortionCalculator` | 8 / 8 | 6 / 6 |
| `ShoppingListGenerator` | 45 / 47 | 25 / 30 |
| `UnitConverter` | 18 / 19 | 19 / 21 |
| `ShoppingListOrganizer` | 68 / 70 | 55 / 66 |
| `InputValidator` | 24 / 24 | 31 / 32 |
| `RecipeFilter` | 26 / 26 | 23 / 24 |
| `DateUtils` | 25 / 32 | 7 / 8 |

The only uncovered method is `DateUtils.today()`. It depends on the current system date, so it is
checked in the manual tests instead (TC-17). The data and UI layers are not covered by unit tests
(see section 1).

---

## 3. Test environment

| Item | Value |
|---|---|
| Device | Android Emulator, Pixel-class phone, 1080 × 2400, Google Play system image |
| Android version | API 37 (Android 17) |
| Build | Debug, `minSdk 24`, `targetSdk 37` |
| Backend | Firebase Authentication (email/password), Cloud Firestore with security rules from `firestore.rules` |
| Development computer | Windows 11, Android Studio, JDK from Gradle toolchain |

---

## 4. Manual system test cases

Fill in the **Result** column during the final test run (✅ pass / ❌ fail, with a note).

### Accounts

| ID | Scenario | Steps | Expected result | Result |
|---|---|---|---|---|
| TC-01 | Registration | Register with name, email, password (6+ chars) and matching repeat | Home shows "Hello, <name>!"; the user appears in Firebase Authentication and `users/{uid}` exists in Firestore | |
| TC-02 | Registration validation | Submit an empty name, an invalid email, a 5-character password, different passwords | Each field shows its own error message; no account is created | |
| TC-03 | Duplicate account | Register again with the same email | "An account with this email already exists" | |
| TC-04 | Login and wrong password | Log out; log in with a wrong password, then a correct one | Wrong: "Incorrect email or password". Correct: Home is shown | |
| TC-05 | Stay logged in | Close the app completely and open it again | Home is shown without logging in | |
| TC-06 | Logout | Settings → Log out → confirm; press Back | Login screen; Back does not return to the app | |
| TC-07 | Data isolation | Create a second account; open Recipes, Planner and Shopping | The second account sees none of the first account's data | |

### Recipes

| ID | Scenario | Steps | Expected result | Result |
|---|---|---|---|---|
| TC-08 | Add recipe | Recipes → + → name, 2 portions, category, description, ingredients "Chicken 200 g", "Rice 100 g" → Save | The recipe appears in the list with "2 portions • 2 ingredients" | |
| TC-09 | Recipe validation | Save with an empty name, 0 or 100 portions, an ingredient without a unit, quantity 0, no ingredients | Clear error messages; nothing is saved | |
| TC-10 | Edit recipe | Open a recipe → Edit → change the name and an ingredient → Save | Details and list show the new values; planned meals show the new name | |
| TC-11 | Portion preview | Open a recipe for 2 portions with 200 g chicken → press + twice | 4 portions: chicken 400 g | |
| TC-12 | Delete recipe | Details → Delete → confirm | The recipe and its planned meals are removed | |
| TC-13 | Favourites | Tap the star in the list; enable the "Favourites" chip | Only favourite recipes are shown; the star is filled | |
| TC-14 | Search | Type part of a name, a category, an ingredient ("onion") | The list shows only matching recipes; "No recipes match" when none match | |
| TC-15 | Unsaved changes | Start a new recipe, type a name, rotate the phone, press Back | The typed text survives rotation; "Discard changes?" is shown | |

### Meal planner

| ID | Scenario | Steps | Expected result | Result |
|---|---|---|---|---|
| TC-16 | Plan a meal | Planner → + on a day → recipe → portions → Save | The meal appears under the chosen day | |
| TC-17 | Today | Plan a meal for today | The day header shows "today"; Home lists the meal under "Today" | |
| TC-18 | Edit and move meal | Tap a meal → change portions and date to next week → Save → › | The meal is shown in the next week with the new portions | |
| TC-19 | Delete meal | Delete icon → confirm | The meal disappears | |
| TC-20 | Week navigation | ‹ › arrows, calendar button, "Go to this week" | The correct week (Monday–Sunday) is shown | |
| TC-21 | Plan from recipe | Recipe details → set 3 portions → "Plan this meal" | The form is pre-filled with the recipe and 3 portions | |

### Shopping list – automatic generation

| ID | Scenario | Steps | Expected result | Result |
|---|---|---|---|---|
| TC-22 | **Specification example** | Recipe A (2 portions): chicken 200 g, rice 100 g. Recipe B (2 portions): chicken 300 g, onion 1 pcs. Plan both for 2 portions this week → Shopping → Generate → this week | **Chicken 500 g, Onion 1 pcs, Rice 100 g**. Chicken appears only once | |
| TC-23 | Portions in generation | Change recipe A's planned portions to 4 → open Shopping | Chicken 700 g, Rice 200 g; "Shopping list updated…" message | |
| TC-24 | Unit normalisation | Recipe with flour 500 g and another with flour 1 kg, both planned → generate | Flour 1.5 kg | |
| TC-25 | No meals in range | Generate for an empty week | "No meals are planned between …"; the list is unchanged | |
| TC-26 | Automatic update | Add, change or delete a planned meal in the generated range → open Shopping | Generated products match the plan; manual products are unchanged | |
| TC-27 | Keep purchased mark | Tick Rice; change a meal that does not use rice → open Shopping | Rice stays ticked; changed products are unticked | |

### Shopping list – manual management

| ID | Scenario | Steps | Expected result | Result |
|---|---|---|---|---|
| TC-28 | Add product | + → "Bread" (no quantity) → Save; + → "Milk 1 l", category Dairy | Both appear, marked "added manually"; Milk under "Dairy" | |
| TC-29 | Product validation | Quantity without unit; unit without quantity; empty name | Error messages; the dialog stays open | |
| TC-30 | Edit product | Tap a product → change quantity / category → Save | The list shows the new values and grouping | |
| TC-31 | Delete and undo | Delete icon → Undo | The product disappears and comes back | |
| TC-32 | Purchased | Tick a product | Crossed out, faded, moved to the end of its group | |
| TC-33 | Clear purchased | "Clear purchased" → confirm | All purchased products are removed | |
| TC-34 | Categories remembered | Give a generated product a category → generate again | The product keeps its category | |

### Non-functional

| ID | Scenario | Steps | Expected result | Result |
|---|---|---|---|---|
| TC-35 | Offline mode | Disable Wi-Fi and mobile data; add a recipe; enable the network again | The offline banner is shown; the recipe is visible at once and synchronised later | |
| TC-36 | Language | Settings → Polski | All texts in Polish, units "szt.", Polish plural forms; kept after a restart | |
| TC-37 | Theme | Settings → Dark / Light / System default | The theme changes at once and is kept after a restart | |
| TC-38 | Rotation | Rotate the phone on every screen | No crash; entered data stays | |
| TC-39 | Security rules | In the Firebase console Rules Playground, read `users/<other uid>/recipes` as a different user | Access denied | |

---

## 5. Requirements traceability matrix

| # | Requirement (specification) | Implementation | Tests |
|---|---|---|---|
| R1 | Create and manage recipes | `RecipesFragment`, `AddEditRecipeActivity`, `RecipeDetailsActivity`, `RecipeRepository` | TC-08, TC-10, TC-12 |
| R2 | Add ingredients to recipes (add/remove while editing) | `AddEditRecipeActivity` (dynamic rows), `Ingredient` | TC-08, TC-09 |
| R3 | Quantities and units g, kg, ml, l, pcs | `MeasurementUnit`, `UnitDimension`, `UnitLabels` | `UnitConverterTest`, TC-08 |
| R4 | Default number of portions | `Recipe.defaultPortions` | TC-08, TC-11 |
| R5 | Plan meals for selected days | `PlannerFragment`, `AddEditPlannedMealActivity`, `MealPlanRepository` | TC-16 – TC-21 |
| R6 | Choose number of portions of a planned meal | `PlannedMeal.portions` | TC-16, TC-18 |
| R7 | Automatic calculation of required quantities | `PortionCalculator` | `PortionCalculatorTest`, TC-11, TC-23 |
| R8 | One combined shopping list from planned meals | `ShoppingListGenerator`, `ShoppingListGenerationService` | `ShoppingListGeneratorTest`, TC-22 |
| R9 | Combine identical ingredients | `ShoppingListGenerator` (key = name + dimension) | `sumsIdenticalIngredients`, `namesAreComparedIgnoringCaseAndSpaces`, TC-22 |
| R10 | Manual management of the list | `ShoppingListFragment`, `ShoppingItemDialog`, `ShoppingListRepository` | TC-28 – TC-33 |
| R11 | Unit conversion of compatible units only | `UnitConverter` | `UnitConverterTest`, `sumsGramsAndKilograms`, `incompatibleUnitsAreNotCombined`, TC-24 |
| R12 | Registration, login, logout (Firebase Authentication) | `AuthRepository`, `LoginActivity`, `RegisterActivity`, `SettingsActivity` | TC-01 – TC-06 |
| R13 | Each user sees only their own data | Firestore path `users/{uid}/…` + `firestore.rules` | TC-07, TC-39 |
| R14 | Recipe: ID, name, description, portions, ingredients, category, favourite | `Recipe` | TC-08, TC-13 |
| R15 | Search recipes, mark favourites | `RecipeFilter`, `RecipeAdapter` | `RecipeFilterTest`, TC-13, TC-14 |
| R16 | Planned meal: ID, recipe ID, recipe name, date, portions | `PlannedMeal` | TC-16 |
| R17 | Generate for a date range | `ShoppingListFragment` (date range picker), `MealPlanRepository.getMealsBetween` | TC-22, TC-25 |
| R18 | Add / edit / delete / mark purchased / clear purchased / categories | `ShoppingListFragment`, `ShoppingListOrganizer` | `ShoppingListOrganizerTest`, TC-28 – TC-34 |
| R19 | Generated and manual products distinguishable | `ItemSource` (GENERATED / MANUAL) | TC-26, TC-28 |
| R20 | Calculation not inside an Activity, separately testable | Package `logic` (no Android imports) | All 77 unit tests run without Android |
| R21 | Layered architecture | Packages `ui`, `data`, `logic`, `model` | See `architecture.md` |
| R22 | Input validation with understandable messages | `InputValidator`, forms, `AuthErrorMapper` | `InputValidatorTest`, TC-02, TC-09, TC-29 |
| R23 | Navigation with BottomNavigationView | `MainActivity` | TC-01, TC-20 |
| R24 | Unit tests (JUnit) for the generation mechanism | `app/src/test/…` | 77 tests, 95 % line coverage of `logic` |

**Extra features (beyond the specification):** English / Polish switch, light / dark theme,
automatic update of the shopping list after changes in the plan, remembered product categories,
offline mode with banner, "today's meals" on Home, unsaved-changes protection, undo after deleting a
product.
