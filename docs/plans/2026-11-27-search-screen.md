# SearchScreen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Реализовать вкладку Search: поиск по полям машины и `note` сервисных записей, группировка записей по машинам, переходы в Car и ServiceEditor.

**Architecture:** Новый `SearchCarsRecordsUseCase` поверх нового `SearchRepository`; FTS5-таблица `records_fts` над `service_records.note` с миграцией БД 1→2; один SQL-запрос с 4 токенами отбирает машины и прицепляет только совпавшие записи; feature/search — MVI на Decompose `Value` и `Flow<Event>`.

**Tech Stack:** Kotlin 2.4.21, Amper, Compose Multiplatform, Decompose 3.6.2, Metro, Room3 3.0.3, FTS5, JUnit4 + kotlin.test.

**Spec:** `docs/specs/2026-11-27-search-screen-design.md`

## Global Constraints

- Только Amper: никаких `settings.gradle*` / `build.gradle*`, `./kotlin`, не `./gradlew`.
- MVI + Decompose: `Value<T>` для состояния, `Flow<Event>` для one-shot, чистый `Reducer`; без ViewModel/ViewModel-Compose.
- `core/domain` без Android, Room, Compose, Decompose.
- Все пользовательские строки — в `core/resources/composeResources/values/strings.xml`.
- Схема БД: версия 2 + явная миграция 1→2; destructive fallback не добавлять.
- Порядок записей — `date DESC`; машин — `createdAt ASC`, затем `id`.
- Санитайз: только `\p{L}`, `\p{N}`, пробелы; максимум 4 слова; FTS-токен — `word*`.
- Финальная проверка — `./kotlin check`.

## Review Focus

- Один токен не должен прицеплять к машине все записи: только записи, чей `note` совпал. Тест — Task 3.
- Токены в разных записях одной машины не должны её терять. Тест — Task 3.
- Машина, найденная по полям, без совпавших записей должна остаться с пустым списком. Тест — Task 3.
- Пустой запрос показывает всю историю, включая машины без записей. Тест — Task 3.
- Миграция существующей БД: `records_fts` создаётся, наполняется и синхронизируется триггерами. Тест — Task 2.
- Ошибка SQLite не должна оставлять экран в Loading/ронять flow. Тесты — Task 1 и Task 5.

---

### Task 1: Domain search contract, tokenizer, use case

**Files:**
- Create: `core/domain/src/search/SearchQuery.kt`
- Create: `core/domain/src/model/CarSearchResult.kt`
- Create: `core/domain/src/error/SearchError.kt`
- Create: `core/domain/src/repository/SearchRepository.kt`
- Create: `core/domain/src/usecase/SearchCarsRecordsUseCase.kt`
- Modify: `core/domain/src/usecase/SearchCarsUseCase.kt`
- Create: `app/test/ru/haykor/carmanager/core/domain/search/SearchQueryTest.kt`
- Create: `app/test/ru/haykor/carmanager/core/domain/usecase/SearchCarsRecordsUseCaseTest.kt`
- Create: `app/test/ru/haykor/carmanager/core/domain/fixtures/FakeSearchRepository.kt`

**Interfaces:**
- Produces:
```kotlin
object SearchQuery {
    fun sanitizeWords(query: String): List<String>
}

data class CarSearchResult(val car: Car, val records: List<ServiceRecordDetails>)

sealed interface SearchError : AppError {
    data class Data(val dataError: DataError) : SearchError
}

interface SearchRepository {
    fun search(words: List<String>): Flow<DataResult<List<CarSearchResult>, DataError>>
}

@Inject
class SearchCarsRecordsUseCase(private val repository: SearchRepository) {
    operator fun invoke(query: String): Flow<DataResult<List<CarSearchResult>, SearchError>>
}
```
- `SearchCarsUseCase` сохраняет прежнее поведение: санитайз через `SearchQuery.sanitizeWords`, FTS-строка `words.joinToString(" ") { "$it*" }`, пустой список → `observeCars()`. Без лимита 4.

- [ ] **Step 1: Write the failing tokenizer tests**

```kotlin
class SearchQueryTest {
    @Test
    fun `keeps unicode letters and digits`() {
        assertEquals(listOf("Лада", "Ves", "5W", "40"), SearchQuery.sanitizeWords("  Лада, Ves! 5W-40 "))
    }

    @Test
    fun `punctuation only yields empty list`() {
        assertEquals(emptyList(), SearchQuery.sanitizeWords("!!! ---"))
    }

    @Test
    fun `collapses any whitespace`() {
        assertEquals(listOf("Toyota", "Camry"), SearchQuery.sanitizeWords("Toyota\n\t  Camry"))
    }
}
```

- [ ] **Step 2: Run tokenizer tests; expect compile failure**

Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.core.domain.search.SearchQueryTest'`
Expected: FAIL — `SearchQuery` unresolved.

- [ ] **Step 3: Implement `SearchQuery` and switch `SearchCarsUseCase` to it**

`SearchQuery.sanitizeWords` replaces `[^\p{L}\p{N} ]` with a space, splits on `\s+`, trims, drops blanks, preserves case. `SearchCarsUseCase` no longer has its own `sanitizeQuery`.

- [ ] **Step 4: Run tokenizer + existing Garage search tests**

Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.core.domain.search.SearchQueryTest'`
Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.core.domain.usecase.SearchCarsUseCaseTest'`
Expected: PASS both.

- [ ] **Step 5: Write the failing use-case test and fake**

`FakeSearchRepository` records `words`, returns a configurable `results` or `error`:
```kotlin
internal class FakeSearchRepository : SearchRepository {
    val searches = mutableListOf<List<String>>()
    var results: List<CarSearchResult> = emptyList()
    var error: DataError? = null

    override fun search(words: List<String>): Flow<DataResult<List<CarSearchResult>, DataError>> = flow {
        searches += words
        emit(error?.let { DataResult.Failure(it) } ?: DataResult.Success(results))
    }
}
```

Tests:
```kotlin
@Test fun `blank query delegates an empty word list`()
@Test fun `sanitizes and caps at four words`() // " a, b c d e f " -> listOf("a","b","c","d")
@Test fun `maps data error to search error`()   // SearchError.Data(DataError.Local.Unknown)
```
Collect with `.first()`.

- [ ] **Step 6: Run use-case tests; expect compile failure**

Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.core.domain.usecase.SearchCarsRecordsUseCaseTest'`
Expected: FAIL — `SearchCarsRecordsUseCase` unresolved.

- [ ] **Step 7: Implement model, error, repository, use case**

`invoke`: `val words = SearchQuery.sanitizeWords(query).take(4)`; return `repository.search(words).map { it.mapError(SearchError::Data) }`.

- [ ] **Step 8: Run all domain search tests**

Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.core.domain.*'`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add core/domain/src app/test/ru/haykor/carmanager/core/domain
git commit -m "feat(search): add cars-and-records search use case"
```

---

### Task 2: `records_fts` schema and migration 1→2

**Review:** per-task — data migration

**Files:**
- Create: `core/data/src/local/entity/service/ServiceRecordFtsEntity.kt`
- Create: `core/data/src/local/migration/Migrations.kt`
- Modify: `core/data/src/local/AppDatabase.kt`
- Modify: `core/data/src/local/di/DatabaseModule.kt`
- Create: `app-debug/test/ru/haykor/carmanager/core/data/local/migration/Migration1To2Test.kt`

**Interfaces:**
```kotlin
@Fts5(contentEntity = ServiceRecordEntity::class)
@Entity(tableName = "records_fts")
data class ServiceRecordFtsEntity(val note: String?)

val MIGRATION_1_2: Migration // androidx.room3.migration.Migration
```
`AppDatabase`: `version = 2`, `ServiceRecordFtsEntity::class` в `entities`. `DatabaseModule`: `.addMigrations(MIGRATION_1_2)`.

- [ ] **Step 1: Write the failing migration test**

Use a temp file + `BundledSQLiteDriver().open(path)`; create only v1 `service_categories` and `service_records` DDL, insert one record with `note = 'Engine oil change'`. Call `MIGRATION_1_2.migrate(connection)` in `runBlocking`. Query helper:
```kotlin
private fun SQLiteConnection.matches(token: String): List<Long> =
    prepare("SELECT rowid FROM records_fts WHERE records_fts MATCH ?").use { stmt ->
        stmt.bindText(1, token)
        buildList { while (stmt.step()) add(stmt.getLong(0)) }
    }
```
Assertions: existing record found by `oil*`; after `UPDATE ... SET note='Brake pads'` — `oil*` empty, `brak*` finds it; after `INSERT` with `note='Tire rotation'` — `tire*` finds the new row; after `DELETE` — no match. Close connection and delete temp file in `finally`.

- [ ] **Step 2: Run migration test; expect compile failure**

Run: `./kotlin test -m app-debug --include-classes 'ru.haykor.carmanager.core.data.local.migration.Migration1To2Test'`
Expected: FAIL — `MIGRATION_1_2` unresolved.

- [ ] **Step 3: Implement entity, migration, version bump, builder migration**

`MIGRATION_1_2` executes the five SQL statements from the spec: `CREATE VIRTUAL TABLE ... FTS5(note, tokenize=unicode61, content=service_records)`, four `room_fts_content_sync_records_fts_*` triggers, then `INSERT INTO records_fts(records_fts) VALUES('rebuild')`.

- [ ] **Step 4: Run migration test**

Run: `./kotlin test -m app-debug --include-classes 'ru.haykor.carmanager.core.data.local.migration.Migration1To2Test'`
Expected: PASS.

- [ ] **Step 5: Build to regenerate schema v2**

Run: `./kotlin build -m app-debug`
Expected: BUILD SUCCESSFUL; `core/data/schemas/ru.haykor.carmanager.core.data.local.AppDatabase/2.json` created.

- [ ] **Step 6: Commit**

```bash
git add core/data/src core/data/schemas app-debug/test/ru/haykor/carmanager/core/data/local/migration
git commit -m "feat(data): index service record notes with FTS5"
```

---

### Task 3: Search query, row mapping, repository

**Files:**
- Create: `core/data/src/model/CarRecordSearchRow.kt`
- Modify: `core/data/src/local/dao/CarDao.kt`
- Create: `core/data/src/mapper/SearchMappers.kt`
- Create: `core/data/src/repository/DefaultSearchRepository.kt`
- Create: `app-debug/test/ru/haykor/carmanager/core/data/local/dao/CarDaoSearchRecordsTest.kt`
- Create: `app-debug/test/ru/haykor/carmanager/core/data/repository/DefaultSearchRepositoryTest.kt`

**Interfaces:**
```kotlin
data class CarRecordSearchRow(
    @Embedded(prefix = "car_") val car: CarEntity,
    @ColumnInfo(name = "record_id") val recordId: ServiceRecordId?,
    @ColumnInfo(name = "record_carId") val recordCarId: CarId?,
    @ColumnInfo(name = "record_atMileage") val atMileage: Long?,
    @ColumnInfo(name = "record_price") val price: Long?,
    @ColumnInfo(name = "record_categoryId") val categoryId: ServiceCategoryId?,
    @ColumnInfo(name = "record_note") val note: String?,
    @ColumnInfo(name = "record_photoUri") val photoUri: String?,
    @ColumnInfo(name = "record_date") val date: Long?,
    @ColumnInfo(name = "record_createdAt") val recordCreatedAt: Long?,
    @ColumnInfo(name = "record_updatedAt") val recordUpdatedAt: Long?,
    @ColumnInfo(name = "record_categoryName") val categoryName: String?,
)
```
`CarDao`: `fun searchCarsAndRecords(t1: String, t2: String?, t3: String?, t4: String?): Flow<List<CarRecordSearchRow>>` with the SQL from spec §SQL-запрос, including the corrected record guard `(:tN IS NOT NULL AND ...)`.

`DefaultSearchRepository(carDao: CarDao) : SearchRepository`:
- `words.isEmpty()` → `carDao.observeAllWithRecords()` + mapper;
- иначе `carDao.searchCarsAndRecords(words[0]+"*", words.getOrNull(1)?.plus("*"), …)`;
- результаты оборачиваются в `DataResult.Success`; `.catch { if (it is SQLiteException) emit(Failure(DataError.Local.Unknown)) else throw it }`.

- [ ] **Step 1: Write failing DAO tests**

Setup mirrors `CarDaoSearchTest`: in-memory Room, `BundledSqliteNative.install()`, drivers, IO context. Insert categories and cars/records through DAOs. Helper `record(carId, categoryId, note, date, mileage)`.

Tests and exact assertions:
- `oneWordQueryDoesNotAttachUnmatchedRecords` — car `Toyota Camry`, records `Engine camry parts` and `Dealer service`; `searchCarsAndRecords("cam*", null, null, null)` → one row, `record_note == "Engine camry parts"` (Camry in car fields), `Dealer service` absent.
- `recordMatchReturnsCarWithOnlyMatchedRecords` — car `Honda Civic`, records `oil change` and `brake pads`; `("oil*", null, …)` → car + only oil record.
- `carMatchWithNoMatchingRecordsReturnsCarWithEmptyRecords` — Camry with `dealer service`; `("cam*", …)` → car present, `recordId == null`.
- `allTokensMustMatchCarOrItsRecords` — Camry with `oil change`; `("cam*", "brak*", …)` → empty.
- `tokensMayMatchDifferentRecordsOfSameCar` — Camry; record A `camry parts` date 10, record B `oil change` date 20; `("cam*", "oil*", …)` → car present, both records, dates `[20, 10]`.
- `recordsMatchingAnyTokenAreReturnedNewestFirst` — car `Honda`; records `camry parts` date 10 and `oil change` date 20; `(“cam*”, “oil*”, …)` → both, `[20, 10]`.
- `recordFtsFollowsInsertUpdateAndDelete` — insert `oil change` → `oil*` finds; update note to `brake pads` → `oil*` empty, `brak*` finds; delete → `brak*` empty.

- [ ] **Step 2: Run DAO tests; expect compile failure**

Run: `./kotlin test -m app-debug --include-classes 'ru.haykor.carmanager.core.data.local.dao.CarDaoSearchRecordsTest'`
Expected: FAIL — `searchCarsAndRecords` unresolved.

- [ ] **Step 3: Implement DTO, DAO query, mappers, repository**

Mapper: `CarRecordSearchRow.toRecordDetails()` returns null when `recordId == null`; otherwise builds `ServiceRecordDetails` with `ServiceCategory(categoryId!!, categoryName.orEmpty())`. `toDomainGroups()` groups by `car.id` in SQL order. `CarWithRecords.toDomainGroup()` sorts `records` by `record.date` descending. Blank-query groups sorted by `car.createdAt`, then `car.id.value`.

- [ ] **Step 4: Run DAO tests**

Run: `./kotlin test -m app-debug --include-classes 'ru.haykor.carmanager.core.data.local.dao.CarDaoSearchRecordsTest'`
Expected: PASS.

- [ ] **Step 5: Write and run repository tests**

`DefaultSearchRepositoryTest` with in-memory Room:
- `blankWordsReturnAllHistory` — car A createdAt 1 with records dates 10 and 20; car B createdAt 2 without records; `repo.search(emptyList()).first()` → `Success([A(records 20,10), B(empty)])`, category names mapped.
- `matchedWordsReturnGroupsWithOnlyMatchedRecords` — one car, two records; `repo.search(listOf("oil")).first()` → only oil record, star appended internally.
- `carWithoutMatchingRecordsIsKept` — Camry, record `dealer`; `repo.search(listOf("camry"))` → group with empty records.

Run: `./kotlin test -m app-debug --include-classes 'ru.haykor.carmanager.core.data.repository.DefaultSearchRepositoryTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/data/src app-debug/test/ru/haykor/carmanager/core/data
git commit -m "feat(data): search cars and records with grouped results"
```

---

### Task 4: Move record card to `core/ui`

**Files:**
- Create: `core/ui/src/ServiceRecordRow.kt`
- Modify: `feature/car/src/ui/CarScreen.kt`

**Interfaces:**
```kotlin
@Composable
fun ServiceRecordRow(
    record: ServiceRecordDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
)
```

- [ ] **Step 1: Move `ServiceRecordCard` and `ServiceAvatar` from `CarScreen.kt` into `core/ui/src/ServiceRecordRow.kt`; rename the public composable to `ServiceRecordRow`. Keep layout, strings, icons, and formatting identical.**

- [ ] **Step 2: Replace the private call in `recordsDetails` with `ServiceRecordRow(...)` and remove now-unused imports/helpers from `CarScreen.kt`.**

- [ ] **Step 3: Build**

Run: `./kotlin build -m app-debug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/ui/src/ServiceRecordRow.kt feature/car/src/ui/CarScreen.kt
git commit -m "refactor(ui): share service record row"
```

---

### Task 5: Search MVI component and root wiring

**Review:** per-task — concurrency

**Files:**
- Create: `feature/search/src/component/SearchContract.kt`
- Modify: `feature/search/src/component/SearchComponent.kt`
- Modify: `feature/search/src/component/DefaultSearchComponent.kt`
- Modify: `app/src/ru/haykor/carmanager/root/component/DefaultRootComponent.kt`
- Create: `app/test/ru/haykor/carmanager/feature/search/component/SearchReducerTest.kt`
- Create: `app/test/ru/haykor/carmanager/feature/search/component/DefaultSearchComponentTest.kt`

**Interfaces:**
```kotlin
@Stable
data class SearchState(
    val query: String = "",
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Results(val groups: List<CarSearchResult>) : Content
    }
}

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object ClearQueryClicked : SearchIntent
    data class CarClicked(val id: CarId) : SearchIntent
    data class RecordClicked(val id: ServiceRecordId) : SearchIntent
}

sealed interface SearchEvent { data class Failed(val error: SearchError) : SearchEvent }

sealed interface SearchMutation {
    data object LoadingStarted : SearchMutation
    data class ResultsLoaded(val groups: List<CarSearchResult>) : SearchMutation
    data class QueryChanged(val query: String) : SearchMutation
    data object LoadFailed : SearchMutation
}

object SearchReducer { fun reduce(state: SearchState, mutation: SearchMutation): SearchState }

interface SearchComponent {
    val state: Value<SearchState>
    val events: Flow<SearchEvent>
    fun accept(intent: SearchIntent)

    fun interface Factory {
        fun create(
            ctx: ComponentContext,
            onOpenCar: (CarId) -> Unit,
            onOpenRecord: (ServiceRecordId) -> Unit,
        ): SearchComponent
    }
}
```
`SearchReducer.LoadFailed`: `Loading` → `Results(emptyList())`; existing `Results` unchanged.
`DefaultSearchComponentFactory` injects `SearchCarsRecordsUseCase`; root passes `onOpenCar = { navigation.pushNew(Config.Car(it)) }`, `onOpenRecord = { navigation.pushNew(Config.ServiceEditor(EditorMode.Edit(it))) }`.
`MainScreen` остаётся с `SearchScreen()` до Task 6.

- [ ] **Step 1: Write reducer tests; run them; expect failure**

Tests:
- `queryChangeUpdatesQuery`
- `resultsReplaceContent`
- `loadFailedFromLoadingBecomesEmptyResults`
- `loadFailedKeepsExistingResults`
Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.feature.search.component.SearchReducerTest'`
Expected: FAIL — contract missing.

- [ ] **Step 2: Implement contract and reducer; run reducer tests**

Run: same command. Expected: PASS.

- [ ] **Step 3: Write component tests using `FakeSearchRepository`**

Setup: `Dispatchers.setMain(UnconfinedTestDispatcher())`, `DefaultComponentContext(LifecycleRegistry())`, `runTest(dispatcher)`.
Tests:
- `initialBlankQueryLoadsHistory` — `advanceTimeBy(301); runCurrent()`; `repo.searches == listOf(emptyList())`; state is `Results`.
- `queryChangeSearchesAfterDebounce` — accept `QueryChanged("Cam, oil!")`; before 301 no new search; after 301 `repo.searches.last() == listOf("Cam", "oil")`.
- `clearQueryLoadsHistory` — after query, `ClearQueryClicked`; last search empty; state query `""`.
- `sameQuerySearchedOnce` — two identical `QueryChanged`; one search.
- `carAndRecordClicksCallCallbacks`.
- `failureEmitsEventAndSettles` — `repo.error = DataError.Local.Unknown`; collect first event in `backgroundScope`; after 301 event is `SearchEvent.Failed(SearchError.Data(...))` and content is `Results(emptyList())` when starting from Loading.

Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.feature.search.component.DefaultSearchComponentTest'`
Expected: FAIL — component missing.

- [ ] **Step 4: Implement `DefaultSearchComponent` and factory; wire root**

`queryFlow.debounce(300.milliseconds).distinctUntilChanged().flatMapLatest { searchCarsRecords(it) }`; `Success` → `ResultsLoaded`; `Failure` → `_events.send(Failed(error))` + `LoadFailed`.

- [ ] **Step 5: Run component tests and build app**

Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.feature.search.component.*'`
Run: `./kotlin build -m app-debug`
Expected: PASS, BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add feature/search/src app/src/ru/haykor/carmanager/root app/test/ru/haykor/carmanager/feature/search
git commit -m "feat(search): add search component with grouped results state"
```

---

### Task 6: Search UI, strings, main screen

**Files:**
- Modify: `feature/search/src/ui/SearchScreen.kt`
- Modify: `feature/search/module.yaml`
- Modify: `core/resources/composeResources/values/strings.xml`
- Modify: `core/ui/src/UiTextMapper.kt`
- Modify: `app/src/ru/haykor/carmanager/feature/main/ui/MainScreen.kt`

**Interfaces:**
```kotlin
@Composable
fun SearchScreen(component: SearchComponent, modifier: Modifier = Modifier)

@Composable
internal fun SearchContent(
    state: SearchState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
)

fun SearchError.toUiText(): StringResource // core/ui
```
Strings:
```xml
<string name="search_empty_history">No cars or records yet</string>
<string name="search_empty_on_query">Nothing found for "%1$s"</string>
```
`feature/search/module.yaml`: add `$libs.decompose.extensions.compose`.

- [ ] **Step 1: Update module dependency and strings**

- [ ] **Step 2: Implement `SearchScreen`**

`Scaffold` + `TopAppBar` с `SearchBar(query = state.query, ...)`; `ObserveAsEvents(component.events)` → Toast `getString(event.error.toUiText())`. `SearchContent`: `Loading` → пусто; `Results` → если `groups.isEmpty()`: `search_empty_history` при `state.query.isBlank()`, иначе `search_empty_on_query(query.trim())`; иначе `LazyColumn`:
```kotlin
groups.forEach { group ->
    item(key = "car_${group.car.id.value}") {
        CarCard(car = group.car, onClick = { onIntent(SearchIntent.CarClicked(group.car.id)) }, modifier = Modifier.padding(horizontal = 16.dp))
    }
    itemsIndexed(group.records, key = { _, r -> r.record.id.value }) { index, record ->
        ServiceRecordRow(
            record = record,
            shape = cmSegmentShape(index, group.records.size),
            onClick = { onIntent(SearchIntent.RecordClicked(record.record.id)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = CmSegmentedGap / 2),
        )
    }
}
```
Add `@Preview` with fake `SearchState(SearchState.Content.Results(...))` inside `AppTheme`.

- [ ] **Step 3: `MainScreen` passes `child.component` to `SearchScreen`**

- [ ] **Step 4: Build and run app tests**

Run: `./kotlin build -m app-debug`
Run: `./kotlin test -m app --include-classes 'ru.haykor.carmanager.feature.search.*'`
Expected: BUILD SUCCESSFUL; PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/search core/resources core/ui/src/UiTextMapper.kt app/src/ru/haykor/carmanager/feature/main/ui/MainScreen.kt
git commit -m "feat(search): add search screen with grouped history"
```

---

### Task 7: Full verification

- [ ] **Step 1: Run all checks**

Run: `./kotlin check`
Expected: PASS (tests + ktlint).

- [ ] **Step 2: Run the app if a device is available**

Run: `./kotlin run -m app-debug` (or `-d <device>`).
Manual: пустой запрос → вся история; `camry` → машина Camry (записи с совпавшим note); `camry масл` → Camry с записями, где есть любое из слов; клик по шапке → Car, по записи → ServiceEditor; back возвращает на Search. Если устройства нет — зафиксировать, что визуальная проверка не выполнена.

- [ ] **Step 3: Commit any generated schema/format changes**

```bash
git add core/data/schemas
git commit -m "chore(data): export schema version 2"
```
