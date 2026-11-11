# Testowanie – Planer posiłków

Dokument opisuje sposób testowania aplikacji: strategię, automatyczne testy jednostkowe wraz
z wynikami i pokryciem kodu, ręczne przypadki testów systemowych oraz macierz śledzenia wymagań,
która wiąże każde wymaganie specyfikacji z jego implementacją i testami.

---

## 1. Strategia testowania

Aplikacja jest testowana na dwóch poziomach.

| Poziom | Przedmiot testów | Narzędzie | Lokalizacja |
|---|---|---|---|
| **Testy jednostkowe** (automatyczne) | Logika biznesowa: przeliczanie porcji, konwersja jednostek, generowanie listy zakupów, porządkowanie listy, walidacja, wyszukiwanie, daty | JUnit 4 na maszynie JVM | `app/src/test/java/.../logic/` |
| **Testy systemowe** (ręczne) | Pełne scenariusze użytkownika na urządzeniu: ekrany, Firebase Authentication, Firestore, praca offline, języki, motywy | Emulator Androida (API 37, obraz z Google Play) | Rozdział 4 niniejszego dokumentu |

**Dlaczego logika jest testowana bez Androida.** Wszystkie obliczenia znajdują się w pakiecie
`logic`, który nie importuje klas `android.*` ani Firebase. Jego testy wykonują się w ciągu
milisekund na komputerze programisty i nie wymagają emulatora ani połączenia sieciowego. Była to
świadoma decyzja projektowa: najważniejszą część projektu, czyli generowanie listy zakupów, można
sprawdzić automatycznie po każdej zmianie.

**Dlaczego warstwy interfejsu i Firebase są testowane ręcznie.** Automatyczne testy tych warstw
wymagałyby Firebase Emulator Suite oraz testów instrumentalnych Espresso. W aplikacji tej
wielkości rozdzielenie warstw w połączeniu z ręcznymi scenariuszami daje wystarczającą pewność
przy znacznie niższym koszcie. Warstwy danych i interfejsu nie zawierają obliczeń, a jedynie
wywołania Firebase i klas z pakietu `logic`.

---

## 2. Testy jednostkowe

### 2.1 Uruchamianie

W Android Studio: kliknij prawym przyciskiem `app/src/test/java` i wybierz **Run 'Tests in …'**.
Z wiersza poleceń:

```bash
./gradlew testDebugUnitTest
```

Raport HTML: `app/build/reports/tests/testDebugUnitTest/index.html`

### 2.2 Wyniki

Wszystkie **77 testów kończy się powodzeniem** (0 niepowodzeń, 0 błędów).

| Klasa testowa | Liczba testów | Testowana klasa | Zakres weryfikacji |
|---|---:|---|---|
| `ShoppingListGeneratorTest` | 20 | `ShoppingListGenerator` | Przykład ze specyfikacji, przeliczanie porcji, łączenie identycznych składników, normalizacja jednostek, przypadki brzegowe |
| `UnitConverterTest` | 12 | `UnitConverter` | Przeliczanie g↔kg i ml↔l, zgodność jednostek, odmowa przeliczenia g→szt., normalizacja do kg / l |
| `PortionCalculatorTest` | 10 | `PortionCalculator` | Wzór `ilość × wybrane / domyślne`, wartości dziesiętne, niepoprawne argumenty |
| `ShoppingListOrganizerTest` | 10 | `ShoppingListOrganizer` | Grupowanie według kategorii, kolejność, zapamiętywanie kategorii, wykrywanie zmian, zachowanie oznaczenia „kupione” |
| `InputValidatorTest` | 9 | `InputValidator` | E-mail, hasło, liczba porcji 1–99, ilości z `.` i `,`, wartości maksymalne |
| `FormatterAndDateUtilsTest` | 9 | `QuantityFormatter`, `DateUtils` | Separator dziesiętny zależny od języka, konwersja dat, początek tygodnia, zakresy dat |
| `RecipeFilterTest` | 7 | `RecipeFilter` | Wyszukiwanie po nazwie / kategorii / składniku, filtr ulubionych, sortowanie |
| **Razem** | **77** | | |

### 2.3 Przypadki testowe wymagane w specyfikacji

| Wymagany przypadek | Test(y) |
|---|---|
| Przeliczanie porcji: 2 porcje, 200 g kurczaka → 4 porcje = 400 g | `PortionCalculatorTest.doublesQuantityWhenPortionsAreDoubled`, `ShoppingListGeneratorTest.recalculatesPortions` |
| Łączenie identycznych składników: 200 g + 300 g = 500 g | `ShoppingListGeneratorTest.sumsIdenticalIngredients`, `combinesTwoRecipesFromSpecification` |
| Różne składniki pozostają osobnymi pozycjami | `ShoppingListGeneratorTest.differentIngredientsStaySeparate` |
| Jednostki miary: 500 g + 1 kg = 1,5 kg | `ShoppingListGeneratorTest.sumsGramsAndKilograms`, `UnitConverterTest.sumOfGramsAndKilogramsInBaseUnit` |
| Niezgodne jednostki nie są przeliczane (g i szt.) | `ShoppingListGeneratorTest.incompatibleUnitsAreNotCombined`, `UnitConverterTest.refusesToConvertGramsToPieces` |
| Różne liczby porcji w kilku przepisach | `ShoppingListGeneratorTest.combinesRecipesWithDifferentPortionCounts` |
| Pusta lista składników | `ShoppingListGeneratorTest.recipeWithoutIngredientsGivesEmptyShoppingList` |
| Jeden zaplanowany posiłek | `ShoppingListGeneratorTest.singlePlannedMeal` |
| Kilka zaplanowanych posiłków | `ShoppingListGeneratorTest.severalPlannedMeals` |
| Ilości dziesiętne | `ShoppingListGeneratorTest.decimalQuantitiesAreSummedAndRounded`, `PortionCalculatorTest.worksWithDecimalQuantities` |

Dodatkowe przypadki brzegowe (wykraczające poza specyfikację): pusta lista posiłków lub wartość
`null`, posiłek, którego przepis został usunięty, posiłek z liczbą porcji 0, niepoprawne składniki
(pusta nazwa, ilość 0, brak jednostki), nazwy różniące się tylko wielkością liter lub spacjami,
ten sam przepis zaplanowany dwukrotnie, zaokrąglanie liczb zmiennoprzecinkowych
(0,1 kg + 0,2 kg = dokładnie 300 g).

### 2.4 Pokrycie kodu

Pokrycie kodu testami jednostkowymi zmierzono za pomocą integracji JaCoCo w Android Gradle Plugin:

```bash
./gradlew createDebugUnitTestCoverageReport
```

Raport HTML: `app/build/reports/coverage/test/debug/index.html`

| Pakiet | Pokrycie linii | Pokrycie rozgałęzień | Pokrycie metod |
|---|---:|---:|---:|
| `logic` (logika biznesowa) | **95,3%** (244 / 256) | **88,5%** (169 / 191) | **98,5%** (64 / 65) |

| Klasa | Linie | Rozgałęzienia |
|---|---:|---:|
| `PortionCalculator` | 8 / 8 | 6 / 6 |
| `ShoppingListGenerator` | 45 / 47 | 25 / 30 |
| `UnitConverter` | 18 / 19 | 19 / 21 |
| `ShoppingListOrganizer` | 68 / 70 | 55 / 66 |
| `InputValidator` | 24 / 24 | 31 / 32 |
| `RecipeFilter` | 26 / 26 | 23 / 24 |
| `DateUtils` | 25 / 32 | 7 / 8 |

Jedyną niepokrytą metodą jest `DateUtils.today()`. Zależy ona od bieżącej daty systemowej, dlatego
jest sprawdzana w testach ręcznych (TC-17). Warstwy danych i interfejsu nie są objęte testami
jednostkowymi (zob. rozdział 1).

---

## 3. Środowisko testowe

| Element | Wartość |
|---|---|
| Urządzenie | Emulator Androida, telefon klasy Pixel, 1080 × 2400, obraz systemu z Google Play |
| Wersja Androida | API 37 (Android 17) |
| Kompilacja | Debug, `minSdk 24`, `targetSdk 37` |
| Zaplecze | Firebase Authentication (e-mail / hasło), Cloud Firestore z regułami z pliku `firestore.rules` |
| Komputer deweloperski | Windows 11, Android Studio, JDK z mechanizmu toolchain Gradle |

---

## 4. Ręczne przypadki testów systemowych

Końcowy przebieg testów 2026-11-11: **wszystkie 39 przypadków testowych zakończyło się wynikiem pozytywnym** (✅ pozytywny / ❌ negatywny).

### Konta użytkowników

| ID | Scenariusz | Kroki | Oczekiwany rezultat | Wynik |
|---|---|---|---|---|
| TC-01 | Rejestracja | Zarejestruj się, podając imię, e-mail, hasło (min. 6 znaków) i jego powtórzenie | Ekran startowy pokazuje „Cześć, <imię>!”; użytkownik pojawia się w Firebase Authentication, a w Firestore istnieje dokument `users/{uid}` | ✅ |
| TC-02 | Walidacja rejestracji | Wyślij formularz z pustym imieniem, błędnym adresem e-mail, hasłem 5-znakowym, różnymi hasłami | Każde pole pokazuje własny komunikat błędu; konto nie zostaje utworzone | ✅ |
| TC-03 | Istniejące konto | Zarejestruj się ponownie tym samym adresem e-mail | „Konto z tym adresem e-mail już istnieje” | ✅ |
| TC-04 | Logowanie i błędne hasło | Wyloguj się; zaloguj się błędnym hasłem, a następnie poprawnym | Błędne: „Nieprawidłowy e-mail lub hasło”. Poprawne: wyświetla się ekran startowy | ✅ |
| TC-05 | Utrzymanie sesji | Całkowicie zamknij aplikację i uruchom ją ponownie | Wyświetla się ekran startowy bez logowania | ✅ |
| TC-06 | Wylogowanie | Ustawienia → Wyloguj → potwierdź; naciśnij Wstecz | Ekran logowania; przycisk Wstecz nie wraca do aplikacji | ✅ |
| TC-07 | Izolacja danych | Utwórz drugie konto; otwórz Przepisy, Planer i Zakupy | Drugie konto nie widzi żadnych danych pierwszego konta | ✅ |

### Przepisy

| ID | Scenariusz | Kroki | Oczekiwany rezultat | Wynik |
|---|---|---|---|---|
| TC-08 | Dodanie przepisu | Przepisy → + → nazwa, 2 porcje, kategoria, opis, składniki „Kurczak 200 g”, „Ryż 100 g” → Zapisz | Przepis pojawia się na liście z opisem „2 porcje • 2 składniki” | ✅ |
| TC-09 | Walidacja przepisu | Zapisz z pustą nazwą, z 0 lub 100 porcjami, ze składnikiem bez jednostki, z ilością 0, bez składników | Zrozumiałe komunikaty błędów; nic nie zostaje zapisane | ✅ |
| TC-10 | Edycja przepisu | Otwórz przepis → Edytuj → zmień nazwę i składnik → Zapisz | Szczegóły i lista pokazują nowe wartości; zaplanowane posiłki mają nową nazwę | ✅ |
| TC-11 | Podgląd porcji | Otwórz przepis na 2 porcje z 200 g kurczaka → naciśnij + dwa razy | 4 porcje: kurczak 400 g | ✅ |
| TC-12 | Usunięcie przepisu | Szczegóły → Usuń → potwierdź | Przepis i jego zaplanowane posiłki zostają usunięte | ✅ |
| TC-13 | Ulubione | Dotknij gwiazdki na liście; włącz filtr „Ulubione” | Widoczne są tylko ulubione przepisy; gwiazdka jest wypełniona | ✅ |
| TC-14 | Wyszukiwanie | Wpisz fragment nazwy, kategorię, nazwę składnika („cebula”) | Lista pokazuje tylko pasujące przepisy; „Żaden przepis nie pasuje do wyszukiwania”, gdy brak wyników | ✅ |
| TC-15 | Niezapisane zmiany | Rozpocznij nowy przepis, wpisz nazwę, obróć telefon, naciśnij Wstecz | Wpisany tekst jest zachowany po obrocie; pojawia się pytanie „Odrzucić zmiany?” | ✅ |

### Planer posiłków

| ID | Scenariusz | Kroki | Oczekiwany rezultat | Wynik |
|---|---|---|---|---|
| TC-16 | Zaplanowanie posiłku | Planer → + przy dniu → przepis → liczba porcji → Zapisz | Posiłek pojawia się pod wybranym dniem | ✅ |
| TC-17 | Dzisiaj | Zaplanuj posiłek na dzisiaj | Nagłówek dnia zawiera „dzisiaj”; ekran startowy pokazuje posiłek w sekcji „Dzisiaj” | ✅ |
| TC-18 | Edycja i przeniesienie posiłku | Dotknij posiłku → zmień liczbę porcji i datę na przyszły tydzień → Zapisz → › | Posiłek jest widoczny w następnym tygodniu z nową liczbą porcji | ✅ |
| TC-19 | Usunięcie posiłku | Ikona usuwania → potwierdź | Posiłek znika | ✅ |
| TC-20 | Nawigacja po tygodniach | Strzałki ‹ ›, przycisk kalendarza, „Przejdź do bieżącego tygodnia” | Wyświetla się właściwy tydzień (poniedziałek–niedziela) | ✅ |
| TC-21 | Planowanie z przepisu | Szczegóły przepisu → ustaw 3 porcje → „Zaplanuj ten posiłek” | Formularz jest wstępnie wypełniony przepisem i 3 porcjami | ✅ |

### Lista zakupów – generowanie automatyczne

| ID | Scenariusz | Kroki | Oczekiwany rezultat | Wynik |
|---|---|---|---|---|
| TC-22 | **Przykład ze specyfikacji** | Przepis A (2 porcje): kurczak 200 g, ryż 100 g. Przepis B (2 porcje): kurczak 300 g, cebula 1 szt. Zaplanuj oba na 2 porcje w tym tygodniu → Zakupy → „Generuj z planu” → bieżący tydzień | **Kurczak 500 g, Cebula 1 szt., Ryż 100 g**. Kurczak występuje tylko raz | ✅ |
| TC-23 | Porcje w generowaniu | Zmień liczbę porcji zaplanowanego przepisu A na 4 → otwórz Zakupy | Kurczak 700 g, Ryż 200 g; komunikat „Lista zakupów została zaktualizowana…” | ✅ |
| TC-24 | Normalizacja jednostek | Przepis z 500 g mąki i drugi z 1 kg mąki, oba zaplanowane → generuj | Mąka 1,5 kg | ✅ |
| TC-25 | Brak posiłków w zakresie | Generuj dla pustego tygodnia | „Brak zaplanowanych posiłków między …”; lista się nie zmienia | ✅ |
| TC-26 | Automatyczna aktualizacja | Dodaj, zmień lub usuń zaplanowany posiłek w zakresie listy → otwórz Zakupy | Produkty wygenerowane odpowiadają planowi; produkty ręczne pozostają bez zmian | ✅ |
| TC-27 | Zachowanie oznaczenia „kupione” | Zaznacz Ryż; zmień posiłek, który nie zawiera ryżu → otwórz Zakupy | Ryż pozostaje zaznaczony; zmienione produkty są odznaczone | ✅ |

### Lista zakupów – zarządzanie ręczne

| ID | Scenariusz | Kroki | Oczekiwany rezultat | Wynik |
|---|---|---|---|---|
| TC-28 | Dodanie produktu | + → „Chleb” (bez ilości) → Zapisz; + → „Mleko 1 l”, kategoria Nabiał i jajka | Oba produkty widoczne z opisem „dodany ręcznie”; Mleko w grupie „Nabiał i jajka” | ✅ |
| TC-29 | Walidacja produktu | Ilość bez jednostki; jednostka bez ilości; pusta nazwa | Komunikaty błędów; okno pozostaje otwarte | ✅ |
| TC-30 | Edycja produktu | Dotknij produktu → zmień ilość / kategorię → Zapisz | Lista pokazuje nowe wartości i grupowanie | ✅ |
| TC-31 | Usunięcie i cofnięcie | Ikona usuwania → Cofnij | Produkt znika i wraca | ✅ |
| TC-32 | Kupione | Zaznacz produkt | Przekreślony, wyszarzony, przeniesiony na koniec swojej grupy | ✅ |
| TC-33 | Usunięcie kupionych | „Usuń kupione” → potwierdź | Wszystkie kupione produkty zostają usunięte | ✅ |
| TC-34 | Zapamiętane kategorie | Nadaj wygenerowanemu produktowi kategorię → wygeneruj ponownie | Produkt zachowuje kategorię | ✅ |

### Wymagania niefunkcjonalne

| ID | Scenariusz | Kroki | Oczekiwany rezultat | Wynik |
|---|---|---|---|---|
| TC-35 | Praca offline | Wyłącz Wi-Fi i dane komórkowe; dodaj przepis; włącz ponownie sieć | Widoczny komunikat o braku połączenia; przepis widoczny od razu i zsynchronizowany później | ✅ |
| TC-36 | Język | Ustawienia → Polski | Wszystkie teksty po polsku, jednostka „szt.”, polskie formy liczby mnogiej; ustawienie zachowane po ponownym uruchomieniu | ✅ |
| TC-37 | Motyw | Ustawienia → Ciemny / Jasny / Zgodny z systemem | Motyw zmienia się od razu i jest zachowany po ponownym uruchomieniu | ✅ |
| TC-38 | Obrót ekranu | Obróć telefon na każdym ekranie | Brak awarii; wprowadzone dane są zachowane | ✅ |
| TC-39 | Reguły bezpieczeństwa | W konsoli Firebase (Rules Playground) odczytaj `users/<inny uid>/recipes` jako inny użytkownik | Odmowa dostępu | ✅ |

---

## 5. Macierz śledzenia wymagań

| Nr | Wymaganie (specyfikacja) | Implementacja | Testy |
|---|---|---|---|
| R1 | Tworzenie przepisów i zarządzanie nimi | `RecipesFragment`, `AddEditRecipeActivity`, `RecipeDetailsActivity`, `RecipeRepository` | TC-08, TC-10, TC-12 |
| R2 | Dodawanie składników do przepisów (dodawanie / usuwanie podczas edycji) | `AddEditRecipeActivity` (dynamiczne wiersze), `Ingredient` | TC-08, TC-09 |
| R3 | Ilości i jednostki g, kg, ml, l, szt. | `MeasurementUnit`, `UnitDimension`, `UnitLabels` | `UnitConverterTest`, TC-08 |
| R4 | Domyślna liczba porcji | `Recipe.defaultPortions` | TC-08, TC-11 |
| R5 | Planowanie posiłków na wybrane dni | `PlannerFragment`, `AddEditPlannedMealActivity`, `MealPlanRepository` | TC-16 – TC-21 |
| R6 | Wybór liczby porcji zaplanowanego posiłku | `PlannedMeal.portions` | TC-16, TC-18 |
| R7 | Automatyczne obliczanie potrzebnych ilości | `PortionCalculator` | `PortionCalculatorTest`, TC-11, TC-23 |
| R8 | Jedna zbiorcza lista zakupów z zaplanowanych posiłków | `ShoppingListGenerator`, `ShoppingListGenerationService` | `ShoppingListGeneratorTest`, TC-22 |
| R9 | Łączenie identycznych składników | `ShoppingListGenerator` (klucz = nazwa + wymiar) | `sumsIdenticalIngredients`, `namesAreComparedIgnoringCaseAndSpaces`, TC-22 |
| R10 | Ręczne zarządzanie listą | `ShoppingListFragment`, `ShoppingItemDialog`, `ShoppingListRepository` | TC-28 – TC-33 |
| R11 | Przeliczanie wyłącznie zgodnych jednostek | `UnitConverter` | `UnitConverterTest`, `sumsGramsAndKilograms`, `incompatibleUnitsAreNotCombined`, TC-24 |
| R12 | Rejestracja, logowanie, wylogowanie (Firebase Authentication) | `AuthRepository`, `LoginActivity`, `RegisterActivity`, `SettingsActivity` | TC-01 – TC-06 |
| R13 | Każdy użytkownik widzi tylko własne dane | Ścieżka Firestore `users/{uid}/…` + `firestore.rules` | TC-07, TC-39 |
| R14 | Przepis: ID, nazwa, opis, porcje, składniki, kategoria, ulubiony | `Recipe` | TC-08, TC-13 |
| R15 | Wyszukiwanie przepisów, oznaczanie ulubionych | `RecipeFilter`, `RecipeAdapter` | `RecipeFilterTest`, TC-13, TC-14 |
| R16 | Zaplanowany posiłek: ID, ID przepisu, nazwa przepisu, data, porcje | `PlannedMeal` | TC-16 |
| R17 | Generowanie dla zakresu dat | `ShoppingListFragment` (selektor zakresu dat), `MealPlanRepository.getMealsBetween` | TC-22, TC-25 |
| R18 | Dodawanie / edycja / usuwanie / oznaczanie kupionych / usuwanie kupionych / kategorie | `ShoppingListFragment`, `ShoppingListOrganizer` | `ShoppingListOrganizerTest`, TC-28 – TC-34 |
| R19 | Rozróżnienie produktów wygenerowanych i ręcznych | `ItemSource` (GENERATED / MANUAL) | TC-26, TC-28 |
| R20 | Obliczenia poza aktywnościami, testowalne niezależnie | Pakiet `logic` (bez importów Androida) | Wszystkie 77 testów jednostkowych działa bez Androida |
| R21 | Architektura warstwowa | Pakiety `ui`, `data`, `logic`, `model` | Zob. `architecture.md` |
| R22 | Walidacja danych ze zrozumiałymi komunikatami | `InputValidator`, formularze, `AuthErrorMapper` | `InputValidatorTest`, TC-02, TC-09, TC-29 |
| R23 | Nawigacja za pomocą BottomNavigationView | `MainActivity` | TC-01, TC-20 |
| R24 | Testy jednostkowe (JUnit) mechanizmu generowania | `app/src/test/…` | 77 testów, 95% pokrycia linii pakietu `logic` |

**Funkcje dodatkowe (wykraczające poza specyfikację):** przełączanie języka angielski / polski,
motyw jasny / ciemny, automatyczna aktualizacja listy zakupów po zmianach w planie, zapamiętywanie
kategorii produktów, praca offline z komunikatem, dzisiejsze posiłki na ekranie startowym, ochrona
przed utratą niezapisanych zmian, cofanie usunięcia produktu.
