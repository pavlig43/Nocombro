# Sync TODO

## Что уже было сделано раньше

- `Room` остается локальной базой для desktop.
- Добавлены служебные таблицы синхронизации:
  - `sync_change`
  - `sync_state`
- Для sync-сущностей введены служебные поля:
  - `syncId`
  - `updatedAt`
  - `deletedAt`
- Single-line формы переведены на sync-шаблон:
  - `Product`
  - `Vendor`
  - `Document`
  - `Declaration`
  - `Transact`
- Collection-формы уже поддерживали sync:
  - `Composition`
  - `ProductDeclaration`
  - `Reminders`
  - `Expenses`
  - `Ingredients`
- `Buy` и `Sale` переведены на multi-entity sync.
- В шапке приложения уже был sync-статус.
- Добавлен локальный `SyncRunner`, который:
  - берет `pending` из `sync_change`
  - помечает записи как `IN_PROGRESS`
  - схлопывает повторы одной сущности в один push-change
  - умеет `markBatchSucceeded` / `markBatchFailed`

## Что реализовано в ветке `120-remote_db`

### Локальный аудит syncable-таблиц

- Проверены все `Room`-таблицы из `NocombroDatabase`.
- Для всех syncable бизнес-сущностей подтверждено наличие:
  - `syncId`
  - `updatedAt`
  - `deletedAt`
- Single-line sync теперь пишет в очередь по `syncId`, а не по локальному `id`.
- Закрыты найденные дыры в write-path:
  - `SafetyStock`
  - standalone `Expense` form
- `SafetyStock` добавлен и в remote pipeline:
  - export payload
  - pull/apply
- Сознательно не включены в remote sync:
  - `FileBD`
  - `BatchCostPriceEntity`
  - `PfBD`

### Remote YDB integration

- Добавлен реальный JDBC gateway для `YDB`:
  - `database/.../sync/YdbJdbcConfig.kt`
  - `database/.../sync/YdbJdbcSyncGateway.kt`
- Добавлен fallback `YdbSyncGatewayMock`, если remote endpoint не настроен.
- В `database/build.gradle.kts` подключен `tech.ydb.jdbc:ydb-jdbc-driver:2.3.21`.
- В DI зарегистрированы:
  - `SyncStateRepository`
  - `SyncEntityExportRepository`
  - `SyncRemoteApplyRepository`
  - `SyncService`
  - `SyncRemoteGateway`

### Push / Pull / Apply

- Добавлен `SyncService` как единая orchestration-точка sync-цикла.
- `syncOnce()` теперь делает:
  - reserve локального batch
  - push локальных изменений
  - pull удаленных изменений
  - apply удаленных изменений в `Room`
  - update `sync_state.lastPushAt`
  - update `sync_state.lastPullAt`
  - update `sync_state.lastRemoteCursor`
- `SyncRemoteGateway` расширен до контракта:
  - `getStatus`
  - `pushChanges`
  - `pullChanges`

### Export payload для remote sync

- Добавлен `SyncEntityExportRepository`.
- Payload больше не собирается вручную через `JsonObjectBuilder`.
- Введены typed `@Serializable` DTO payload-модели:
  - `VendorSyncPayload`
  - `DocumentSyncPayload`
  - `DeclarationSyncPayload`
  - `ProductSyncPayload`
  - `SafetyStockSyncPayload`
  - `CompositionSyncPayload`
  - `ProductDeclarationSyncPayload`
  - `BatchSyncPayload`
  - `BatchMovementSyncPayload`
  - `BuySyncPayload`
  - `SaleSyncPayload`
  - `ReminderSyncPayload`
  - `ExpenseSyncPayload`
  - `TransactionSyncPayload`
- Export теперь идет через `kotlinx.serialization`.
- Связи в remote payload переведены с локальных `id` на стабильные `syncId`.

### Versioned payload contract

- Добавлен `SyncPayloadEnvelope<T>`.
- Введена константа `CURRENT_SYNC_PAYLOAD_VERSION = 1`.
- Новый payload уходит в формате:
  - `version`
  - `payload`
- `pull/apply` поддерживает:
  - новый envelope-формат
  - старый raw-DTO формат для обратной совместимости

### Apply удаленных изменений в локальную БД

- Добавлен `SyncRemoteApplyRepository`.
- Реализовано применение remote changes в `Room` с учетом порядка зависимостей:
  - `Vendor`
  - `Document`
  - `Product`
  - `SafetyStock`
  - `Transact`
  - `Declaration`
  - `Batch`
  - `ProductDeclaration`
  - `Composition`
  - `BatchMovement`
  - `Reminder`
  - `Expense`
  - `Buy`
  - `Sale`
- Базовая конфликтная стратегия сейчас:
  - `updatedAt`
  - last-write-wins

### DAO и data-layer подготовка

- Для sync/export/apply добавлены выборки по `sync_id`.
- Для нужных сущностей добавлены дополнительные DAO-методы чтения по локальному `id`, чтобы строить payload на `syncId`-ссылках.
- Подготовлены DAO для export/import следующих сущностей:
  - `Vendor`
  - `Document`
  - `Declaration`
  - `Product`
  - `SafetyStock`
  - `Transact`
  - `Composition`
  - `ProductDeclaration`
  - `Batch`
  - `BatchMovement`
  - `Buy`
  - `Sale`
  - `Reminder`
  - `Expense`

### Desktop debug UI

- `SyncComponent` переведен на `SyncService`.
- В top bar dropdown добавлены диагностические поля:
  - `payloadVersion`
  - `lastPullAt`
  - `lastRemoteCursor`
  - `lastError`
- Добавлена кнопка `Скопировать`, которая кладет в clipboard debug snapshot:
  - `sync.remote_configured`
  - `sync.payload_version`
  - `sync.pending`
  - `sync.failed`
  - `sync.has_remote_changes`
  - `sync.last_sync_at`
  - `sync.last_pull_at`
  - `sync.last_status_check_at`
  - `sync.last_remote_cursor`
  - `sync.last_error`

## Что сознательно еще не доведено

- Нет реального прогона на живом remote `YDB`.
- Remote схема пока промежуточная:
  - одна универсальная таблица
  - `payload_json`
- Нет полноценного production-grade conflict resolution.
- Нет отдельного reset/replay механизма для cursor.
- Нет sync файлов.

## Текущая runtime-конфигурация YDB

Для включения реального JDBC gateway используются:

- `NOCOMBRO_YDB_JDBC_URL` или JVM property `nocombro.ydb.jdbcUrl`
- `NOCOMBRO_YDB_TOKEN` или JVM property `nocombro.ydb.token`
- `NOCOMBRO_YDB_SYNC_TABLE` или JVM property `nocombro.ydb.syncTable`

Если `jdbcUrl` не задан, приложение работает через `YdbSyncGatewayMock`.

## Решение по удаленной БД

- `YDB` выбрана как основная удаленная БД.
- Локальная база остается в `Room/SQLite`.
- Локальный sync-слой нужен как подготовительный слой между UI/очередью и remote `YDB`.

## Следующий практический шаг

1. Получить реальный remote `YDB` endpoint.
2. Подставить `jdbcUrl`, `token`, `table`.
3. Прогнать sync на живых данных.
4. По результатам первого прогона уточнить:
   - remote schema
   - conflict policy
   - обработку edge cases

## Коммиты по sync

- `e7c75a44` Add sync base repositories for single forms
- `328e8ebf` Clarify single sync repository contracts
- `274f3891` Add sync support for simple collection forms
- `560d0323` Add sync support for product composition
- `de46b16d` Add sync support for product declarations
- `f267f6cc` Add sync support for OPZS ingredients
- `d71febd1` Register sync queue in form scopes
- `4782522d` Preserve sync ids for buy and sale rows
- `73a7f2fc` Add multi-entity sync for transaction rows
- `ebcd3707` Add sync queue runner
- `85a4795e` Add YDB sync pipeline and desktop debug UI
