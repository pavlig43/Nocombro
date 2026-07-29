# YDB Mirror Sync

## Архитектура

Синхронизация работает только через typed mirror tables в YDB.

- Каждая business table имеет отдельную remote-таблицу.
- Ключ строки: `sync_id`.
- Межтабличные ссылки хранятся через `*_sync_id`.
- Победитель определяется по `updated_at` / `deleted_at`.
- Версии `updated_at` и `deleted_at` записываются в UTC и монотонно растут даже
  при откате системных часов.
- В схеме они остаются строками `LocalDateTime` без часового пояса. Код трактует
  только эти версии синхронизации как UTC. Пользовательские даты и время сохраняют
  прежнюю локальную семантику.
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
- `money_account`
- `money_movement`

Инвентарь таблиц находится в
`database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorSyncTable.kt`.

DDL находится в `database/ydb/mirror_sync_v1.sql`. Приложение не создаёт
mirror tables во время работы. Таблицы нужно создать или мигрировать заранее
через SQL.

Для складов `MAIN`/`EXPERIMENTAL` перед выпуском клиента нужно применить
`database/ydb/migrate_storage_locations_v2.sql`. Пустое значение в поле
`storage_location` читается как `MAIN`. Старые клиенты нужно обновить до начала
создания транзакций `STORAGE_TRANSFER`.

Подробные правила складских операций и расчётов описаны в
[`docs/STORAGE_LOCATIONS.md`](docs/STORAGE_LOCATIONS.md).

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
  отправляет local winners в mirror tables. YDB принимает строку только тогда,
  когда её версия не старше входящей; принятые и отклонённые строки возвращаются
  отдельно.
- На desktop новый файл сначала копируется в каталог Nocombro и сохраняется в
  Room с заранее созданными `sync_id` и `remote_object_key`. Сеть для сохранения
  формы не нужна.
- Перед записью локальных file-победителей в YDB sync загружает их бинарные копии
  в S3. При ошибке одного файла весь пакет YDB и последующий pull не запускаются.
  Следующий ручной sync повторяет S3 → YDB по той же более новой строке Room.
- Tombstone и старые строки `file` без `remote_object_key` в S3 не отправляются.
  Push без файлов не требует настроенного S3.
- После отклонённого push клиент один раз заново загружает remote snapshot и
  повторяет reconciliation. Если локальная версия всё ещё новее, выполняется одна
  повторная условная запись. Второе отклонение завершает операцию ошибкой.
- Pull применяет remote winners локально.
- Равные версии с разными данными считаются конфликтом. Doctor позволяет выбрать
  локальную или удалённую строку и создаёт новую версию выбранного варианта.
- После pull отсутствующие бинарные файлы догружаются из S3.
- Doctor показывает доступность mirror tables и расхождения local/remote.
- Doctor S3 cleanup сравнивает S3 с активными строками mirror `file`, локальными
  файлами и старым реестром незавершённых загрузок. Новые записи в этот реестр
  больше не создаются. Перед удалением remote snapshot загружается повторно.
  Cleanup блокируется при ошибке sync, локальных изменениях и старых блокировках.
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
