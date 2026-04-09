# Sync TODO

## Что уже сделано

- `Room` остается локальной базой для desktop.
- Добавлены служебные таблицы синхронизации:
  - `sync_change`
  - `sync_state`
- Для sync-таблиц введены служебные поля:
  - `syncId`
  - `updatedAt`
  - `deletedAt`
- Single-line формы переведены на обязательный sync-шаблон:
  - `Product`
  - `Vendor`
  - `Document`
  - `Declaration`
  - `Transact`
- Collection-формы уже поддерживают sync:
  - `Composition`
  - `ProductDeclaration`
  - `Reminders`
  - `Expenses`
  - `Ingredients`
- `Buy` и `Sale` переведены на multi-entity sync:
  - `Sale` синкает `sale + batch_movement`
  - `Buy` синкает `buy + batch_movement + batch`
- Для `buy/sale` сохранены стабильные `syncId` в `Out -> UI -> In`.
- В шапке приложения уже есть sync-статус и периодическая проверка статуса.
- Добавлен локальный `SyncRunner`, который:
  - берет `pending` из `sync_change`
  - помечает записи как `IN_PROGRESS`
  - схлопывает повторы одной сущности в один push-change
  - умеет `markBatchSucceeded` / `markBatchFailed`

## Что сознательно отложено

- Синхронизация файлов.
- Реальный серверный `push/pull`.
- Автоматическое применение удаленных изменений в локальную БД.

## Следующий шаг

Ближайший следующий шаг по sync:

1. заменить mock `YDB` gateway на реальный adapter
2. реализовать настоящий `push` подготовленного `RemotePushPayload`
3. сохранять и использовать серверный cursor/offset в `sync_state`
4. добавить реальный `pull/status` поверх того же gateway

Текущий локальный слой уже делает:

- DI-регистрацию `SyncRunner` и sync-service
- запуск sync по кнопке
- обработку `sync_change`
- обновление `sync_state.lastPushAt`
- подготовку payload для удаленного backend
- fallback между `YdbSyncGatewayMock` и реальным `YdbJdbcSyncGateway`

## Текущая runtime-конфигурация YDB

Для включения реального JDBC gateway сейчас используются:

- `NOCOMBRO_YDB_JDBC_URL` или JVM property `nocombro.ydb.jdbcUrl`
- `NOCOMBRO_YDB_TOKEN` или JVM property `nocombro.ydb.token`
- `NOCOMBRO_YDB_SYNC_TABLE` или JVM property `nocombro.ydb.syncTable`

Если `jdbcUrl` не задан, приложение остается на mock gateway.

## Решение по удаленной БД

На текущий момент принято такое решение:

- `YDB` выбрана как основная удаленная БД
- текущий вектор синхронизации строится вокруг интеграции с `YDB`
- архитектуру локального sync-слоя все равно держим достаточно чистой, чтобы не смешивать UI, очередь и конкретный backend

При этом важно:

- локальная база все равно остается в `Room/SQLite`
- очередь `sync_change` и `SyncRunner` остаются локальной точкой подготовки изменений перед отправкой в удаленное хранилище
- интеграция с `YDB` должна опираться на этот слой, а не обходить его напрямую

## Текущий целевой вектор

- локально: `Room/SQLite`
- удаленно: `YDB`
- серверный стек сейчас предполагается как:
  - `YDB`
  - sync API/adapter над `YDB`
  - файлы отдельно

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
