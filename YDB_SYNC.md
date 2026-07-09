# YDB Mirror Sync

## Архитектура

Синхронизация работает только через typed mirror tables в YDB.

- Каждая business table имеет отдельную remote-таблицу.
- Ключ строки: `sync_id`.
- Межтабличные ссылки хранятся через `*_sync_id`.
- Победитель определяется по `updated_at` / `deleted_at`.
- Удаления распространяются как tombstone через `deleted_at`.
- Бинарные файлы хранятся в S3, в YDB синхронизируются только metadata.

Legacy transport удалён:

- нет `sync_push_log`;
- нет cursor paging;
- нет legacy gateway или mock fallback;
- нет queue runner и JSON remote payload;
- `SyncService` выполняет только mirror reconciliation.
- локальная таблица `sync_change` удаляется миграцией Room 6 -> 7;
- legacy-колонка `sync_state.last_remote_cursor` удаляется той же миграцией;
- typed mirror rows применяются к Room напрямую, без legacy payload-моделей.

## Mirror Tables

- `file`
- `document`
- `vendor`
- `declaration`
- `product`
- `composition`
- `product_declaration`
- `product_specification`
- `safety_stock`
- `batch`
- `batch_cost_price`
- `batch_movement`
- `transact`
- `buy`
- `sale`
- `reminder`
- `expense`
- `experiment`
- `experiment_entry`
- `experiment_reminder`

Инвентарь таблиц находится в
`database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorSyncTable.kt`.

DDL находится в `database/ydb/mirror_sync_v1.sql`. Приложение не создаёт
mirror tables во время работы. Таблицы нужно создать или мигрировать заранее
через SQL.

Старые remote-таблицы приложение не удаляет. После выпуска новой версии,
успешного push/pull и проверки восстановления их можно удалить вручную SQL из
`database/ydb/drop_legacy_sync_tables.sql`.

## Локальные удаления

Физические удаления и SQLite cascade проходят через
`mirror_deletion_journal`.

Перед удалением сохраняются typed mirror rows, затем локальный snapshot
возвращает их как tombstone. Это позволяет отправлять удаления в порядке
child -> parent, не меняя существующее UI-поведение.

## Настройки

- `NOCOMBRO_YDB_JDBC_URL` или `nocombro.ydb.jdbcUrl`
- `NOCOMBRO_YDB_SA_FILE` или `nocombro.ydb.saFile`
- `NOCOMBRO_YDB_TOKEN` или `nocombro.ydb.token`
- `NOCOMBRO_YDB_MIRROR_ROOT` или `nocombro.ydb.mirrorRoot`

Если `jdbcUrl` не задан, используется default database URL.

На Windows service-account key по умолчанию также ищется в:

```text
%APPDATA%\Nocombro\ydb-sa-key.json
```

## Runtime

- Push сравнивает local и remote snapshots, применяет remote winners локально и
  отправляет local winners в mirror tables.
- Pull применяет remote winners локально.
- После pull отсутствующие бинарные файлы догружаются из S3.
- Doctor показывает доступность mirror tables и расхождения local/remote.
- Doctor S3 cleanup сравнивает S3 только с активными строками mirror `file` и
  повторно загружает mirror непосредственно перед удалением.
- Восстановление пустой Room-БД выполняется обычной кнопкой синхронизации.

## Удаление legacy tables

1. Установить новую версию на все рабочие машины.
2. Проверить, что status показывает доступный mirror без ошибки.
3. Выполнить push/pull и убедиться, что local и mirror совпадают в Doctor.
4. Проверить восстановление тестовой пустой Room-БД.
5. Сделать резервную копию или экспорт старых YDB-таблиц.
6. Выполнить `database/ydb/drop_legacy_sync_tables.sql` вручную в YDB Query.

Приложение этот SQL не запускает.

## Проверки

Обычные локальные тесты:

```powershell
.\gradlew :database:desktopTest
.\gradlew :rootnocombro:desktopTest
```

Реальный YDB smoke test:

```powershell
$env:NOCOMBRO_YDB_SMOKE = "true"
.\gradlew :database:desktopTest --tests "ru.pavlig43.database.YdbMirrorIntegrationTest" --no-parallel
```

Disaster recovery test:

```powershell
$env:NOCOMBRO_YDB_DISASTER_RECOVERY = "true"
.\gradlew :database:desktopTest --tests "ru.pavlig43.database.YdbMirrorDisasterRecoveryTest" --no-parallel
```

## Известное ограничение

YDB `UPSERT` пока не защищён server-side сравнением версии. Поздний push
устаревшего snapshot теоретически может перезаписать более новую строку.
Отложенная задача: https://github.com/pavlig43/Nocombro/issues/131
