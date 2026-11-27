# AGENTS.md — CarManager

Android/Kotlin app for car and service-record tracking. Local-first, offline, Room. No backend.

## Non-negotiables

- Build system is **Kotlin Toolchain (Amper)**, not Gradle. Never add `settings.gradle*` / `build.gradle*`; never run `./gradlew`.
- UI stack is **Compose Multiplatform + Decompose + MVI**. No ViewModel, no Navigation-Compose, no Hilt/Koin.
- State reaches UI as Decompose `Value<T>`. One-shot effects go through `Flow<Event>`.
- Every state change goes through a pure `Reducer.reduce(state, mutation)`.
- `core/domain` is pure Kotlin: no Android, Room, Compose, or Decompose imports.
- User-visible strings live in `core/resources/composeResources/values/strings.xml`; no hardcoded text.
- Run `./kotlin check` before calling work done.

## Build & run

Wrapper: `./kotlin` (Kotlin CLI 0.14.1; provisions JDK 21). Project layout: `project.yaml` + per-module `module.yaml`.

| Task | Command |
| --- | --- |
| Build all | `./kotlin build` |
| Build module | `./kotlin build -m app-debug` |
| Build release | `./kotlin build -m app-release -v release` |
| Unit tests | `./kotlin test -m app` |
| One test class | `./kotlin test -m app --include-classes 'ru.haykor.carmanager.feature.garage.*'` |
| All checks (tests + ktlint) | `./kotlin check` |
| Ktlint only | `./kotlin check ktlintCheck -m car` |
| Format | `./kotlin do ktlintFormat` |
| Run debug app | `./kotlin run -m app-debug` |
| Run on device | `./kotlin run -m app-debug -d <adb-device-id>` |
| Inspect project | `./kotlin show modules\|tasks\|checks\|commands\|settings\|dependencies` |
| Clean | `./kotlin clean` |

Toolchain: Kotlin 2.4.21, JDK 21, AGP 9.2.1, compileSdk 37, targetSdk 36, minSdk 26.
Libraries: Compose BOM 2026.09.00, Material3 1.5.0-beta02, Decompose 3.5.0, Room3 3.0.3, Metro 1.5.2, KSP 2.3.12.
Version catalog: root `libs.versions.toml`; referenced as `$libs.*`. Compose deps come from built-in `$compose.*` aliases, not the catalog.

## Modules

| Module | Type | Responsibility | Depends on |
| --- | --- | --- | --- |
| `app` | kmp/lib | Shell: `BaseApp`, `MainActivity`, root stack, main tabs, `AppGraph` | all `feature/*`, `core/*` |
| `app-debug` | android/app | Debug application, debug graph, demo seeder | `app`, `core/data` |
| `app-release` | android/app | Release application, release graph, category seeder, signing | `app`, `core/data` |
| `core/common` | kmp/lib | `StartupTask`, `ComprehensionDebt` | — |
| `core/result` | kmp/lib | `DataResult`, `EmptyResult`, `AppError` | — |
| `core/domain` | kmp/lib | Models, IDs, parsers, validators, repo interfaces, use cases | `common`, `result` |
| `core/data` | kmp/lib | Room3 DB, DAOs, entities, mappers, repo implementations | `domain`, `result` |
| `core/presentation` | kmp/lib | `createComponentScope`, `ObserveAsEvents` | Decompose, lifecycle, coroutines |
| `core/design-system` | kmp/lib | Generic Compose components, no domain types | `common`, `resources` |
| `core/ui` | kmp/lib | App-specific UI: `CarCard`, theme, formatters, error mapping | `design-system`, `domain`, `resources` |
| `core/resources` | kmp/lib | Compose resources (strings, drawables, fonts) | — |
| `feature/*` | kmp/lib | One screen/flow per module: contract + component + UI | `core/*` only |
| `plugins/ktlint` | jvm/amper-plugin | `ktlintCheck`, `ktlintFormat` | — |

Dependency rules:

- `feature/*` never depends on another `feature/*`.
- `core/domain` never depends on `core/data`, `core/ui`, or Android.
- `core/data` never depends on UI.
- `app` is a library, not an application module; only `app-debug` / `app-release` are `android/app`.
- Mark a dependency `exported` only when its types appear in this module's public API (`api` vs `implementation`).
- Adding a module: create `<path>/module.yaml`, add path to `project.yaml`, apply `//common.module-template.yaml` + `//ktlint.module-template.yaml` (+ `//metro.module-template.yaml` if DI, compose settings if UI), add dependency edges, add it to `app/module.yaml` if a feature.
- Targets are Android-only (`platforms: [android]`). Do not add JVM/desktop/iOS targets without an explicit request.
- `-XXLanguage:+ExplicitBackingFields` is enabled only in `feature/car` and `feature/serviceEditor`. If you use `override val state field = ...` in a new module, add it to `settings.kotlin.freeCompilerArgs`.

Feature `module.yaml` template:

```yaml
product:
  type: kmp/lib
  platforms: [android]

dependencies:
  - //core/design-system
  - //core/domain: exported
  - //core/presentation: exported
  - //core/resources
  - //core/ui: exported
  - $libs.decompose.extensions.compose
  - $compose.preview

dependencies@android:
  - $compose.uiTooling

apply:
  - //common.module-template.yaml
  - //metro.module-template.yaml
  - //ktlint.module-template.yaml

settings:
  compose: enabled
```

## Source layout

- Amper layout: `<module>/src/...`, not `<module>/src/main/kotlin/...`.
- Feature: `feature/<name>/src/component/` (contract, interface, default) and `feature/<name>/src/ui/`.
- Unit tests: `app/test/ru/haykor/carmanager/...` — all modules' unit tests live in `app`.
- `app-debug`-only tests (Room/FTS): `app-debug/test/ru/haykor/carmanager/...`. Kotlin Toolchain 0.14 has no instrumented (device) test source set, so these run on the host JVM.
- Test deps are declared in `app/module.yaml` (`test-dependencies`) or `app-debug/module.yaml`, not per-feature.
- Generated code: `build/`, `.kotlin/`, `Res` — never edit.
- `local.properties`, `app-release/keystore.properties`, `*.jks`, `*.keystore` are gitignored; never commit them.

## UI architecture: MVI + Decompose

```
UI event -> Intent -> Component.accept -> side effect / Mutation -> Reducer -> State (Value)
                                                    |
                                              Event (Flow) -> UI one-shot effect
```

Rules:

- `State` is immutable: `@Stable` data class or sealed hierarchy. Computed props (`isDirty`, `isBusy`) are allowed.
- Model exclusive/modal states with `sealed interface`, not boolean soup. Split large screens into section states.
- `Intent` = user actions. `Mutation` = reducer inputs. `Event` = one-shot effects (toast, snackbar).
- `Reducer` is a pure `object`; no suspend, no flows, no callbacks, no Android. Exhaustive `when`.
- Component: `accept(intent)` maps to `reduce(...)` and/or `scope.launch`. Reduce only after the result.
- Navigation is a callback (`onBack`, `onOpenCar`), not an `Event`. Events are for transient UI.
- One-shot events: `Channel<Event>(Channel.BUFFERED).receiveAsFlow()`; collect with `ObserveAsEvents`.
- Do not put events in state; do not collect events in `LaunchedEffect` directly.
- Search/debounce: internal `MutableStateFlow` + `debounce(300.milliseconds)` + `distinctUntilChanged()` + `flatMapLatest`.
- Component scope: `createComponentScope()` = `SupervisorJob + Dispatchers.Main.immediate`, cancelled on destroy.
- Back handling: `BackCallback`, register in `init`, unregister via `lifecycle.doOnDestroy`. Dirty + not busy -> dialog; busy -> leave. `SearchBar` consumes back while focused or while the query is non-empty, clearing query/focus first. `DefaultRootComponent` registers a back callback that, when Main is active and any tab other than Garage is selected, switches to Garage; the `SearchBar` handler wins first while the query is active.
- `exit()` must be idempotent: `isExitHandled` + `lifecycle.state == DESTROYED` guard; prevents double pop after async save.
- Forms: keep `initialInput`, `touched`, `errors`; validate on change/blur; `AllFieldsTouched` on save; parser returns `Valid` / `Invalid`.
- UI is stateless: `Screen(component)` adapter + `Content(state, onIntent)`; previews call `Content` with fake state.
- Repositories/use cases are called from components, never from Composables.

### New component skeleton

`FooContract.kt`:

```kotlin
@Stable
data class FooState(val isLoading: Boolean = true) {
    sealed interface Content {
        data object Loading : Content
        data class Loaded(val items: List<Foo>) : Content
    }
}

sealed interface FooIntent {
    data object RetryClicked : FooIntent
}

sealed interface FooEvent {
    data class Failed(val error: DataError) : FooEvent
}

sealed interface FooMutation {
    data class Loaded(val items: List<Foo>) : FooMutation
}

object FooReducer {
    fun reduce(state: FooState, mutation: FooMutation): FooState = when (mutation) {
        is FooMutation.Loaded -> state.copy(isLoading = false)
    }
}
```

`FooComponent.kt`:

```kotlin
interface FooComponent {
    val state: Value<FooState>
    val events: Flow<FooEvent>
    fun accept(intent: FooIntent)

    fun interface Factory {
        fun create(ctx: ComponentContext, onBack: () -> Unit): FooComponent
    }
}
```

`DefaultFooComponent.kt`:

```kotlin
class DefaultFooComponent(
    componentContext: ComponentContext,
    private val repo: FooRepository,
    private val onBack: () -> Unit,
) : FooComponent, ComponentContext by componentContext {

    private val scope = createComponentScope()
    private val _state = MutableValue(FooState())
    override val state: Value<FooState> = _state
    private val _events = Channel<FooEvent>(Channel.BUFFERED)
    override val events = _events.receiveAsFlow()

    override fun accept(intent: FooIntent) = when (intent) {
        FooIntent.RetryClicked -> load()
    }

    private fun load() = scope.launch {
        when (val result = repo.load()) {
            is DataResult.Success -> reduce(FooMutation.Loaded(result.data))
            is DataResult.Failure -> _events.send(FooEvent.Failed(result.error))
        }
    }

    private fun reduce(mutation: FooMutation) {
        _state.update { FooReducer.reduce(it, mutation) }
    }
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultFooComponentFactory(private val repo: FooRepository) : FooComponent.Factory {
    override fun create(ctx: ComponentContext, onBack: () -> Unit) =
        DefaultFooComponent(ctx, repo, onBack)
}
```

`FooScreen.kt`:

```kotlin
@Composable
fun FooScreen(component: FooComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()
    val context = LocalContext.current
    ObserveAsEvents(component.events) { event ->
        when (event) {
            is FooEvent.Failed -> Toast.makeText(context, getString(event.error.toUiText()), Toast.LENGTH_SHORT).show()
        }
    }
    FooContent(state, component::accept, modifier)
}

@Composable
internal fun FooContent(state: FooState, onIntent: (FooIntent) -> Unit, modifier: Modifier = Modifier) {
    // Scaffold, top bar, content; all text via stringResource(Res.string...)
}
```

### Decompose wiring

- `MainActivity` creates the root with `retainedComponent { ctx -> appGraph.rootComponentFactory.create(ctx) }`.
- Root navigation: `StackNavigation<Config>` + `childStack(..., handleBackButton = true)`.
- Every `Config` is `@Serializable`; arguments (`CarId`, `EditorMode`) are `@Serializable` value/sealed types.
- Child rendering: `RootContent` uses `Children` + `predictiveBackAnimation`; add a branch for every `RootComponent.Child`.
- Tabs: `MainTab` enum + `PagesNavigation<Config>` + `childPages`; UI uses `ChildPages` + `HorizontalPager(userScrollEnabled = false)`.
- Add a new route: `Child` variant -> serializable `Config` variant -> `createChild` branch -> `RootContent` branch -> factory in `DefaultRootComponentFactory`.
- Add a new tab: `MainTab` entry -> `Child` + `Config` -> `DefaultMainComponent.child()` -> `MainScreen` branch -> `topLevelDestinations`.
- No ViewModel, no `SavedStateHandle`, no Navigation-Compose. Decompose own retention and back handling.

## DI: Metro

- `@Inject` on classes (constructor injection). No manual factories unless the object needs runtime args.
- Interface binding: `@Inject @ContributesBinding(AppScope::class) class DefaultFoo(...) : Foo`.
- Provider modules: `@ContributesTo(AppScope::class) interface FooModule { @Provides fun ... }`.
- Singleton: `@Provides @SingleIn(AppScope::class)` (see `DatabaseModule`).
- Set contributions: `@ContributesIntoSet(AppScope::class)` (see `StartupTask`, `PlateFormat`).
- Graph: `@DependencyGraph(AppScope::class)` in `app-debug` / `app-release`, factory provides `Context`.
- `AppGraph` exposes only `rootComponentFactory` and `startupTasks`; add graph-level bindings to both graphs or a shared `@ContributesTo` module.
- `Clock` is injected from `AppModule`; never call `Clock.System` in domain/data/feature code.
- Do not add Hilt, Koin, Dagger, or a service locator.

## Layers

### domain

- Pure Kotlin. Models, value-class IDs, repository interfaces, use cases, parsers, validators.
- IDs: `@Serializable @JvmInline value class CarId(val value: String)` with `CarId.random()` via `kotlin.uuid.Uuid`.
- Use cases: `@Inject class XUseCase(...)` + `operator fun invoke(...)`.
- Parsers/validators: `validate(input): Map<Field, FieldError>` + `parse(input): Valid/Invalid`; normalize once and use the same values for validation and saving.
- Errors: `DataError` for storage; feature-specific `AppError` subtypes for use-case failures (`SaveRecordError`).
- Results: `DataResult<D, E>` / `EmptyResult<E>`; use `map`, `mapError`, `getOrReturn`, `fold`.
- No Android imports. No `Clock.System`. No UI strings.

### data

- Room3 + `BundledSQLiteDriver`; DB file `app.db`; query context `Dispatchers.IO`.
- Repository implementations: `@Inject @ContributesBinding(AppScope::class)`, map entities to domain models.
- `safeCall { }` wraps writes: rethrows `CancellationException`, maps `SQLiteException` to `DataError.Local.Unknown`.
- Observing model reads return `Flow<T>` without `DataResult`; the DB is the source of truth. Writes return `DataResult`. Search is the exception: `SearchRepository` returns `Flow<DataResult<List<CarSearchResult>, DataError>>` because FTS/DB failures must reach the UI.
- Entities use domain value-class IDs directly; mappers live in `core/data/mapper`.
- Partial updates: `@Update(entity = XEntity::class)` + `CarUpdate` / `ServiceRecordUpdate`; do not hand-build `ContentValues`.
- Multi-write invariants: `@Transaction` methods on an `abstract class` DAO (see `ServiceRecordDao`).
- Schema: `AppDatabase` version 2, `exportSchema = true`, `room.schemaLocation = ./schemas`. Change schema -> bump version + migration; never rely on destructive fallback.
- FTS5: `CarFtsEntity` (`cars_fts`) over brand/model/plate/note; `ServiceRecordFtsEntity` (`records_fts`) over `service_records.note`, synced by Room triggers.
- Search sanitization lives in domain (`SearchQuery.sanitizeWords`): keep letters/digits, split words, max 4 in `SearchCarsRecordsUseCase`; the repository appends `*`. Garage's `SearchCarsUseCase` keeps no 4-word cap.
- Foreign keys: car -> records `CASCADE`; category -> records `RESTRICT`.
- `ServiceRecordDao.mileageBounds` / `latestDate` provide the SQL facts; mileage ordering rules belong in use cases, not UI.

### presentation

- Only shared Decompose/coroutines plumbing. No feature code, no domain logic.
- `ObserveAsEvents` collects with `repeatOnLifecycle(STARTED)` and `rememberUpdatedState`.

### UI

- `AppTheme` = `MaterialExpressiveTheme` + `MotionScheme.expressive()`; wrap screens/previews.
- Design system first: `FormTextField`, `CmNavRow`, `CmSectionHeader`, `UnsavedChangesDialog`, `LicensePlate`, `cmSegmentShape`, `CmSegmentedGap`.
- `core/ui` for app-specific pieces: `CarCard`, `ServiceRecordRow`, theme, `formatNumber`, `formatDate`, `Car.title`, `DataError.toUiText()`.
- Field errors are mapped to strings in the feature UI (`CarFieldError.message`, `RecordFieldError.message`).
- Icons are Compose resources (`Res.drawable.*`), not `material-icons`.
- Lists: `LazyColumn` + stable `key`; segmented shapes via `cmSegmentShape(index, count)`.
- Edge-to-edge is enabled in `MainActivity`; respect Scaffold insets. `windowSoftInputMode=adjustResize`.
- Forms: always `FormTextField` (handles error/hint + real focus loss). Keep focus/back behavior local to UI.
- Previews: `@Preview` + `AppTheme` + fake state; no components, no repositories.
- `SharedTransitionLayout` in `RootContent` wraps the whole app in a `LookaheadScope`. Do not animate a layout-affecting modifier such as `padding` there: the child's size updates every frame, but its placement stays frozen until the spring reverses, so the element visibly jumps sideways (the search-bar flicker regression). Animate the size with `BoxWithConstraints` + an animated `Modifier.width` on a centered `Box` (`width = maxWidth - inset * 2`) — size and position then change in the same pass. See `SearchBar` in `core/ui/src/SearchBar.kt`.

## Product scope

- Implemented: Garage list/search, car details, car create/edit/delete, service-record create/edit/delete, mileage sequence validation, category defaults, search over car fields and record notes with records grouped by car.
- Not implemented yet: reminders, reports, documents, photo picker, cloud sync, accounts/roles.
- `photoUri` fields and DB columns exist, but no picker/upload flow. Do not claim photo support.
- Local-only MVP: no backend, no auth, no network layer. Do not add Retrofit/Ktor without an explicit request.

## Testing

- Unit tests: JUnit4 + `kotlin.test` asserts. No MockK/Mockito; use hand-written fakes in `app/test/.../fixtures`.
- Component test: `DefaultComponentContext(LifecycleRegistry())`, `Dispatchers.setMain(UnconfinedTestDispatcher())`, `runTest(dispatcher)`.
- Debounce: `advanceTimeBy(301)` + `runCurrent()`.
- Reducer tests: pure state-in/state-out, one behavior per test.
- Use-case tests: fakes for repositories, assert `DataResult.Failure` type and that nothing was persisted.
- Room/FTS tests: JVM tests in `app-debug/test`, in-memory DB + `BundledSQLiteDriver`; call `BundledSqliteNative.install()` in `@Before` so the Android driver loads the host native library (`sqlite-bundled-jvm` provides it in `test-dependencies`).
- Search DAO/repository tests and `Migration1To2Test` (v1→v2, `records_fts`) live in `app-debug/test`; app-level Search tests live in `app/test`.
- Add a regression test at the lowest layer that can express the bug; no UI-only coverage for domain rules.
- `./kotlin check` runs `tests` + `ktlintCheck`; a failing check blocks done.

## Style

- ktlint: `android_studio`, 4 spaces, max line 120, LF, final newline, no trailing whitespace.
- Import layout: `*, java.**, javax.**, kotlin.**, ^`; wildcard-import rule is disabled.
- `.editorconfig` is the source of truth; do not reformat unrelated code.
- Sealed `when` must be exhaustive. Prefer immutable data + `copy`.
- Naming: `DefaultXComponent`, `XContract`, `XReducer`, `XScreen`; composables PascalCase, `cm*` design-system composables are the exception (ktlint ignores `@Composable` naming).
- Comments explain why, not what. Russian and English comments both exist; match the file.
- `@ComprehensionDebt` marks non-obvious AI-generated code that needs human review; add it when you generate tricky logic.

## Debug vs release

- `app-debug`: applicationId `ru.haykor.carmanager.debug`; `DatabaseSeeder` inserts demo cars/records.
- `app-release`: applicationId `ru.haykor.carmanager`; seeds service categories only; signing via gitignored `keystore.properties`.
- `BaseApp` runs `StartupTask`s on `Dispatchers.IO` at startup; add seeds as `@ContributesIntoSet(AppScope::class)` `StartupTask`s.
- Never put demo data in the release seeder. Never hardcode signing secrets.

## Do not

- Do not add Gradle files, `./gradlew`, or Gradle-only plugins.
- Do not add ViewModel, Navigation-Compose, Hilt/Koin, Retrofit, or a backend without an explicit request.
- Do not expose `StateFlow` to UI; expose `Value<T>`.
- Do not put one-shot events in state.
- Do not call repositories from Composables or reducers.
- Do not catch `CancellationException`.
- Do not use `GlobalScope` or `runBlocking` in app code.
- Do not add Android `res/` to feature modules; use `core/resources/composeResources`.
- Do not hardcode user-visible strings, dates, or numbers.
- Do not edit `build/`, `.kotlin/`, generated `Res`, or `core/data/schemas` by hand.
- Do not add a module without updating `project.yaml`.
