# Meal Planner

Android application for planning meals and automatically generating a shopping list.

Engineering project: *Projekt i implementacja aplikacji mobilnej do planowania posiłków i zarządzania listą zakupów*
(specialisation: Software Engineering and Mobile Technologies).

## Features

- Registration, login and logout (Firebase Authentication)
- Recipes with ingredients (quantity + unit: g, kg, ml, l, pcs), default number of portions, category, favourites, search
- Recipe details with recalculation of quantities for any number of portions
- Weekly meal planner with a chosen number of portions per meal
- **Automatic shopping list**: quantities recalculated for the planned portions, compatible units converted
  (500 g + 1 kg = 1.5 kg), identical ingredients merged into one product. The list updates itself when the plan changes
- Manual products, editing, "purchased" marking, clearing purchased products, grouping by category
- English / Polish, light / dark theme, offline mode

## Technologies

Java 11 · XML layouts · Material Components 3 · Firebase Authentication · Cloud Firestore · JUnit 4 · Gradle (AGP 9.5)

`minSdk 24` (Android 7.0) · `targetSdk 37`

## Project structure

```
app/src/main/java/pl/projectinz/mealplanner/
├── model/   data classes (Recipe, Ingredient, PlannedMeal, ShoppingListItem, …)
├── logic/   business logic in pure Java (ShoppingListGenerator, PortionCalculator, UnitConverter, …)
├── data/    Firebase access (repositories, ShoppingListGenerationService)
└── ui/      Activities, Fragments, adapters
app/src/test/java/…/logic/   JUnit tests of the business logic
docs/        architecture, Firestore design, test plan
firestore.rules              Firestore security rules
```

## Documentation

- [Architecture](docs/architecture.md): layers, classes, data model, the shopping-list algorithm, design decisions
- [Firestore structure](docs/firestore.md): collections, fields, justification, security rules
- [Testing](docs/test-plan.md): unit tests and coverage, manual test cases, requirements traceability

Polish versions for the thesis (wersje polskie):
[Architektura](docs/pl/architecture.md) ·
[Struktura Firestore](docs/pl/firestore.md) ·
[Testowanie](docs/pl/test-plan.md)

## Running the project

1. Open the project folder in **Android Studio**.
2. Create a Firebase project in the [Firebase console](https://console.firebase.google.com):
   - add an Android app with the package name `pl.projectinz.mealplanner`,
   - download `google-services.json` and put it into the `app/` folder,
   - enable **Authentication → Sign-in method → Email/Password**,
   - create a **Firestore Database** (production mode) and paste the content of `firestore.rules` into its **Rules** tab.
3. Run the `app` configuration on an emulator with a **Google Play** system image or on a phone.

## Tests

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

Generate the code-coverage report (`app/build/reports/coverage/test/debug/index.html`):

```bash
./gradlew createDebugUnitTestCoverageReport
```

Current result: **77 tests, all passing**. Line coverage of the business logic: **95 %**.
