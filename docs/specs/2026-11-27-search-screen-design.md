# SearchScreen: поиск по машинам и сервисным записям

Дата: 2026-11-27
Статус: на утверждении

## Зачем

Вкладка Search сейчас — пустая заглушка. Нужен поиск, который находит и машины, и их сервисные записи, и показывает записи, сгруппированные по машинам: запрос `camry масл` возвращает машины, где `camry` есть в полях машины или в `note` записи, а `масл` — там же; под каждой найденной машиной видны только записи, чей `note` совпал хотя бы с одним словом запроса.

## Что уже есть

- Garage уже ищет по машинам: `SearchCarsUseCase` → `CarRepository.searchCars` → `CarDao.searchCars` → FTS5-таблица `cars_fts` (`brand`, `model`, `plate`, `note`, `tokenize=unicode61`, external content `cars`).
- `cars_fts` синхронизируется триггерами Room; обновления и удаления проверены `CarDaoSearchTest`.
- FTS-таблицы для записей нет.
- `feature/search` содержит только пустые `SearchComponent`, `DefaultSearchComponent` и `SearchScreen`; `MainScreen` вызывает `SearchScreen()` без компонента.
- Запись отображается карточкой `ServiceRecordCard` (категория, дата, пробег, цена, note); она приватна в `feature/car`, а `feature/*` не может зависеть от другого `feature/*`.
- Навигация из Search должна идти через `RootComponent`: `Config.Car(carId)` и `Config.ServiceEditor(EditorMode.Edit(recordId))`.

## Решения

### Слои и контракты

Новый сценарий, а не расширение `SearchCarsUseCase`: у Garage результат — `List<Car>`, у Search — машины с записями и ошибка. Расширение сломало бы Garage или потребовало union-тип.

- `core/domain`
  - `CarSearchResult(car: Car, records: List<ServiceRecordDetails>)`.
  - `SearchError : AppError` с `data class Data(val dataError: DataError)` — по образцу `SaveRecordError.Data`.
  - `SearchRepository.search(words: List<String>): Flow<DataResult<List<CarSearchResult>, DataError>>`. Пустой список слов — вся история.
  - `SearchCarsRecordsUseCase(query): Flow<DataResult<List<CarSearchResult>, SearchError>>`; санитайзит, режет на слова, берёт первые 4, делегирует репозиторию и маппит `DataError` в `SearchError`.
  - Общий чистый хелпер `SearchQuery.sanitizeWords(query): List<String>`: оставить `\p{L}`, `\p{N}` и пробелы, схлопнуть пробелы, вернуть слова без `*`. `SearchCarsUseCase` переводится на него без изменения поведения: сам добавляет `*`, лимита в 4 слова там нет.
- `core/data`
  - `DefaultSearchRepository` с `@Inject @ContributesBinding(AppScope::class)`.
  - Пустой запрос: `CarDao.observeAllWithRecords()` (существующая `@Relation`-выборка) → домен; записи сортируются по `date DESC`, машины по `createdAt`, затем `id`.
  - Непустой: новый `CarDao.searchCarsAndRecords(t1, t2, t3, t4)`.
  - Ошибки SQLite во Flow: `.catch { if (it is SQLiteException) emit(Failure(DataError.Local.Unknown)) else throw it }`; `CancellationException` не глотать.
- FTS-токены: репозиторий превращает слово в `"$word*"` (префиксный поиск, как в Garage). Санитайзер уже удалил спецсимволы FTS.

### Семантика поиска

- Санитайз: только буквы/цифры/пробелы; пустой результат — «весь список».
- Не больше 4 слов; пятое и далее отбрасываются в юзкейсе.
- Условие машины: **для каждого** слова — слово найдено в `cars_fts` этой машины **или** хотя бы в одной её записи в `records_fts`. Между словами — AND.
- Записи под машиной: только те, чей `note` совпал хотя бы с одним словом (OR), `date DESC`. Записи, не совпавшие ни одним словом, не показываются.
- Если машина прошла по своим полям, а совпавших записей нет — машина остаётся с пустым списком записей.
- Машина не обязана быть Camry: если `camry` встретился только в `note` другой машины, она тоже результат.
- Пустой запрос: все машины и все их записи.
- Категории в FTS не участвуют; фильтр по категориям — отдельная будущая фича.

### Схема БД и миграция

Новая FTS5-таблица `records_fts` — external content над `service_records(note)`:

```kotlin
@Fts5(contentEntity = ServiceRecordEntity::class)
@Entity(tableName = "records_fts")
data class ServiceRecordFtsEntity(val note: String?)
```

`AppDatabase` — версия 2, сущность добавлена в `entities`. Миграция `MIGRATION_1_2` (Room3 `Migration`, `SQLiteConnection`):

```sql
CREATE VIRTUAL TABLE IF NOT EXISTS `records_fts`
  USING FTS5(`note`, tokenize=`unicode61`, content=`service_records`);
CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_records_fts_BEFORE_UPDATE
  BEFORE UPDATE ON `service_records` BEGIN
    DELETE FROM `records_fts` WHERE `rowid`=OLD.`rowid`;
  END;
CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_records_fts_BEFORE_DELETE
  BEFORE DELETE ON `service_records` BEGIN
    DELETE FROM `records_fts` WHERE `rowid`=OLD.`rowid`;
  END;
CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_records_fts_AFTER_UPDATE
  AFTER UPDATE ON `service_records` BEGIN
    INSERT INTO `records_fts`(`rowid`, `note`) VALUES (NEW.`rowid`, NEW.`note`);
  END;
CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_records_fts_AFTER_INSERT
  AFTER INSERT ON `service_records` BEGIN
    INSERT INTO `records_fts`(`rowid`, `note`) VALUES (NEW.`rowid`, NEW.`note`);
  END;
INSERT INTO `records_fts`(`records_fts`) VALUES('rebuild');
```

Триггеры обязательны: Room создаёт их только на свежей БД, миграция должна создать сама. `DatabaseModule` добавляет `.addMigrations(MIGRATION_1_2)`. Схема 2 экспортируется генератором, руками не правится.

### SQL-запрос

Отдельно: условие отбора машины считается по полному набору её записей, а join фильтрует только показываемые записи. Иначе машина с токенами в разных записях пропадёт.

```sql
SELECT
  c.id AS car_id, c.brand AS car_brand, c.model AS car_model, c.year AS car_year,
  c.note AS car_note, c.vin AS car_vin, c.plateCountry AS car_plateCountry,
  c.plate AS car_plate, c.mileage AS car_mileage, c.photoUri AS car_photoUri,
  c.createdAt AS car_createdAt, c.updatedAt AS car_updatedAt,
  r.id AS record_id, r.carId AS record_carId, r.atMileage AS record_atMileage,
  r.price AS record_price, r.categoryId AS record_categoryId, r.note AS record_note,
  r.photoUri AS record_photoUri, r.date AS record_date,
  r.createdAt AS record_createdAt, r.updatedAt AS record_updatedAt,
  sc.name AS record_categoryName
FROM cars c
LEFT JOIN service_records r
  ON r.carId = c.id
 AND (
   r.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t1)
   OR (:t2 IS NOT NULL AND r.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t2))
   OR (:t3 IS NOT NULL AND r.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t3))
   OR (:t4 IS NOT NULL AND r.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t4))
 )
LEFT JOIN service_categories sc ON sc.id = r.categoryId
WHERE (
  c.rowid IN (SELECT rowid FROM cars_fts WHERE cars_fts MATCH :t1)
  OR EXISTS (
    SELECT 1 FROM service_records r1
    WHERE r1.carId = c.id
      AND r1.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t1)
  )
)
AND (:t2 IS NULL OR (
  c.rowid IN (SELECT rowid FROM cars_fts WHERE cars_fts MATCH :t2)
  OR EXISTS (
    SELECT 1 FROM service_records r2
    WHERE r2.carId = c.id
      AND r2.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t2)
  )
))
AND (:t3 IS NULL OR (... :t3 ...))
AND (:t4 IS NULL OR (... :t4 ...))
ORDER BY c.createdAt, c.id, r.date DESC
```

Сравнение идёт по `rowid`, а не по `c.id`/`r.id`: `CarId`/`ServiceRecordId` — TEXT-UUID, а FTS `rowid` — INTEGER.

Null-guard для записей — `(:tN IS NOT NULL AND ...)`, не `(:tN IS NULL OR ...)`. Иначе при 1–3 словах join прицепит к машине все записи, а не только совпавшие. Null-guard в условии отбора машины, наоборот, `(:tN IS NULL OR ...)`: отсутствующее слово не должно отбрасывать машину.

DAO возвращает `Flow<List<CarRecordSearchRow>>`; DTO хранит `@Embedded(prefix = "car_") car: CarEntity` и плоские nullable-поля записи (`record_id`, `record_note`, …, `record_categoryName`). Маппер строит `ServiceRecordDetails` только при непустом `record_id`; группировка — `LinkedHashMap` по `car.id` с сохранением порядка SQL.

### Feature: MVI и UI

`feature/search`:

- `SearchContract.kt`: `@Stable SearchState(query: String = "", content: Content)`; `SearchState.Content.Loading | Results(groups: List<CarSearchResult>)`; `SearchIntent.QueryChanged | ClearQueryClicked | CarClicked(CarId) | RecordClicked(ServiceRecordId)`; `SearchEvent.Failed(SearchError)`; `SearchMutation.LoadingStarted | ResultsLoaded | QueryChanged | LoadFailed`; `SearchReducer` — чистый объект.
- `DefaultSearchComponent`: `MutableValue` + `Channel<SearchEvent>`; `queryFlow.debounce(300ms).distinctUntilChanged().flatMapLatest { useCase(it) }`; на `Failure` — событие и `LoadFailed` (при `Loading` переходит в пустой `Results`, существующие результаты не очищает); `CarClicked`/`RecordClicked` зовут колбэки.
- `SearchComponent.Factory.create(ctx, onOpenCar: (CarId) -> Unit, onOpenRecord: (ServiceRecordId) -> Unit)`.
- `SearchScreen(component)`: `subscribeAsState`, `ObserveAsEvents` → Toast через новый `SearchError.toUiText()` в `core/ui`, `SearchContent(state, onIntent)`.
- UI: `Scaffold` + `SearchBar` в `TopAppBar`; `LazyColumn` по группам: шапка — `CarCard(car, onClick)` с горизонтальным отступом 16dp, ниже — строки записей `ServiceRecordRow` с `cmSegmentShape(index, count)` и отступом; ключи — `car.id.value` / `record.id.value`. Пустые состояния: нет истории → «пока ничего нет»; нет результатов по запросу → «по запросу ничего не найдено»; ошибка — toast, экран остаётся в текущем состоянии. Preview с фейковым состоянием.
- `ServiceRecordCard`/`ServiceAvatar` переносятся из `feature/car` в `core/ui/src/ServiceRecordRow.kt` как публичный `ServiceRecordRow(record, onClick, modifier, shape)`; `CarScreen` использует его. Дублирование UI запрещено, `feature/search` не зависит от `feature/car`.
- `feature/search/module.yaml`: добавить `$libs.decompose.extensions.compose` (нужен `subscribeAsState`); `core/design-system` уже добавлен в рабочем дереве.

### Навигация

- `DefaultRootComponent`: для Search передать `onOpenCar = { navigation.pushNew(Config.Car(it)) }`, `onOpenRecord = { navigation.pushNew(Config.ServiceEditor(EditorMode.Edit(it))) }`.
- `MainScreen`: `SearchScreen(child.component)`.
- Возврат назад из Car/ServiceEditor возвращает на Search; состояние Search (запрос, результаты) сохраняется Decompose-страницей.

### Тесты

- Domain (`app/test`):
  - `SearchQueryTest`: пунктуация, Unicode, пробелы, пустой запрос.
  - `SearchCarsRecordsUseCaseTest`: пустой запрос → пустой список слов; санитайз; обрезка до 4; маппинг `DataError` → `SearchError.Data`.
  - Существующий `SearchCarsUseCaseTest` остаётся зелёным (поведение Garage не меняется).
- Data (`app-debug/test`):
  - `CarDaoSearchRecordsTest` (in-memory Room): слово в поле машины → машина только с совпавшими записями и с пустым списком, если записей нет; слово в `note` → машина и только совпавшая запись; AND по двум словам, включая случай «слова в разных записях одной машины»; OR по записям (`camry` в одной, `масл` в другой) и порядок `date DESC`; запись без совпадений не попадает; FTS синхронизируется после insert/update/delete записи.
  - `DefaultSearchRepositoryTest` (in-memory Room): группировка, категория в доменной модели, порядок, пустой запрос — вся история.
  - `Migration1To2Test`: на «сырой» SQLite-базе со схемой v1 выполнить `MIGRATION_1_2`, проверить `MATCH` по существующим/новым/изменённым/удалённым записям.
- Feature (`app/test`):
  - `SearchReducerTest`.
  - `DefaultSearchComponentTest`: debounce 300мс, initial пустой запрос → история, смена/очистка запроса, переходы, колбэки навигации, ошибка → событие и состояние.
- `./kotlin check`.

## Не входит

- Фильтр по категориям (следующая фича).
- Поиск по названию категории, VIN, году, цене.
- Подсветка совпадений, пагинация, выбор сортировки.
- Изменение поиска Garage или `SearchCarsUseCase` по поведению.
- Новые зависимости, кроме перечисленных.

## Мои решения (можно оспорить)

- Порядок: машины `createdAt ASC` (как в Garage), записи `date DESC`; не `c.id`, потому что UUID-порядок произволен.
- Ошибка чтения: toast `error_unknown`, результаты не очищаются; отдельного error-экрана и retry нет.
- Строки: новые `search_empty_history`, `search_empty_on_query`; `SearchError.toUiText()` в `core/ui`.
- Общая карточка записи переезжает в `core/ui`, а не дублируется в Search.
