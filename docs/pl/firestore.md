# Struktura bazy danych Firestore

Wszystkie dane należą do konkretnego użytkownika i są przechowywane pod jego dokumentem.
Identyfikatorem tego dokumentu jest UID nadawany przez Firebase Authentication.

```
users/{uid}                              User
users/{uid}/recipes/{recipeId}           Recipe (składniki osadzone w dokumencie)
users/{uid}/mealPlans/{plannedMealId}    PlannedMeal
users/{uid}/shoppingItems/{itemId}       ShoppingListItem (jedna aktywna lista)
```

## Dokumenty

| Kolekcja | Pola | Klasa Java |
|---|---|---|
| `users` | `email`, `displayName`, `createdAt`, `shoppingListFrom`, `shoppingListTo` (zakres dat bieżącej listy zakupów) | `model.User` |
| `recipes` | `name`, `description`, `defaultPortions` (int), `category`, `favourite` (bool), `ingredients` (tablica obiektów `{name, quantity, unit}`), `createdAt` | `model.Recipe`, `model.Ingredient` |
| `mealPlans` | `recipeId`, `recipeName`, `date` (`"yyyy-MM-dd"`), `portions` (int), `createdAt` | `model.PlannedMeal` |
| `shoppingItems` | `name`, `quantity` (0 = brak ilości), `unit` (opcjonalne), `category`, `purchased` (bool), `source` (`GENERATED`/`MANUAL`), `createdAt` | `model.ShoppingListItem` |

Pole `unit` jest zapisywane jako nazwa stałej typu wyliczeniowego: `G`, `KG`, `ML`, `L`, `PCS`.
Identyfikator dokumentu nie jest przechowywany jako pole. Podczas odczytu Firestore kopiuje go
do pola oznaczonego adnotacją `@DocumentId`.

## Decyzje projektowe

1. **Dane zagnieżdżone pod `users/{uid}`.** Jedna reguła bezpieczeństwa
   (`request.auth.uid == userId`) gwarantuje, że każdy użytkownik ma dostęp wyłącznie
   do własnych przepisów, planu posiłków i listy zakupów (zob. plik `firestore.rules`).
2. **Składniki osadzone w przepisie.** Składniki są zawsze odczytywane i zapisywane razem
   z przepisem, a jeden przepis zawiera ich niewiele (znacznie poniżej limitu 1 MiB na dokument).
   Jeden odczyt zwraca kompletny przepis, a jego zapis jest pojedynczą, atomową operacją.
3. **Nazwa przepisu skopiowana do zaplanowanego posiłku (denormalizacja).** Planer może
   wyświetlać posiłki bez wczytywania wszystkich przepisów. Po zmianie nazwy przepisu nowa
   nazwa jest kopiowana do jego zaplanowanych posiłków w jednej operacji `WriteBatch`.
   Usunięcie przepisu powoduje usunięcie jego zaplanowanych posiłków. Jeżeli posiłek mimo to
   wskazuje na nieistniejący przepis, generator go pomija i informuje o tym użytkownika.
4. **Daty jako tekst w formacie `"yyyy-MM-dd"`.** Zaplanowany posiłek dotyczy dnia, a nie
   konkretnego momentu w czasie. Teksty w tym formacie sortują się tak samo jak daty, dlatego
   zapytanie `whereGreaterThanOrEqualTo("date", from).whereLessThanOrEqualTo("date", to)`
   wybiera zakres dat bez problemów ze strefami czasowymi.
5. **Jedna aktywna lista zakupów jako płaska kolekcja.** Każdy produkt jest osobnym
   dokumentem, więc oznaczenie produktu jako kupionego zmienia jeden mały dokument.
   Ponowne wygenerowanie listy usuwa tylko produkty z `source == GENERATED` i zachowuje
   produkty dodane ręcznie (operacje wykonywane w paczkach `WriteBatch` liczących najwyżej
   450 operacji). Zakres dat listy jest zapisany w `users/{uid}`, dzięki czemu lista jest
   automatycznie przeliczana po zmianie planu posiłków.
6. **Liczby.** Ilości mają typ `double` (dozwolone są wartości dziesiętne, np. 0,5 kg),
   a liczby porcji typ `int`.

## Reguły bezpieczeństwa

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
