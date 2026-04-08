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

Подключить `SyncRunner` к приложению:

1. зарегистрировать его в DI
2. добавить mock/local executor для кнопки sync
3. по кнопке реально обрабатывать `sync_change`
4. обновлять `sync_state.lastPushAt`

После этого можно делать уже настоящий серверный контракт:

- `POST /sync/push`
- `GET /sync/pull`
- `GET /sync/status`

## Решение по удаленной БД

На текущий момент принято такое решение:

- `YDB` не выбрана как основная цель
- причина: если потом уходить на свой сервер с `Postgres`, это будет отдельная миграция между разными технологиями
- если нужен переносимый путь, лучше целиться в серверную схему уровня `Postgres`

При этом важно:

- локальная база все равно остается в `Room/SQLite`
- даже если временно использовать другой удаленный backend, данные можно заново пересинхронизировать с локальных клиентов
- но чтобы не делать лишнюю серверную работу дважды, лучше не завязываться глубоко на `YDB`

## Текущий целевой вектор

- локально: `Room/SQLite`
- удаленно позже: свой сервер
- серверный стек пока предполагается как:
  - `Postgres`
  - тонкий sync API
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
