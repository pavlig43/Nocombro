# Отчет о переходе на YDB Mirror Sync

## Статус документа

Документ описывает текущие незакоммиченные изменения в ветке `128-s3error`.

- Коммит не создан.
- Файлы не добавлялись в staging в рамках подготовки этого отчета.
- Основная тема изменений: замена legacy queue-based синхронизации на typed mirror sync между Room/SQLite и YDB.
- Дополнительные темы: восстановление файлов из S3, безопасная очистка S3, обновление Doctor, адаптация форм и cloud-функции напоминаний.

## Краткое резюме

Старая схема синхронизации строилась вокруг универсального журнала `sync_push_log`,
локальной очереди `sync_change`, JSON payload, remote cursor и набора классов для
экспорта, push, pull и применения удаленных изменений.

Новая схема синхронизирует актуальные снимки бизнес-таблиц:

1. Для каждой синхронизируемой Room-таблицы существует typed mirror table в YDB.
2. Строки сопоставляются по стабильному `sync_id`.
3. Связи между сущностями передаются через `*_sync_id`, а не через локальные Room `id`.
4. Победившая версия определяется по `updated_at` и `deleted_at`.
5. Удаления передаются как tombstone.
6. Push отправляет локальные победившие версии.
7. Pull применяет удаленные победившие версии в Room.
8. После pull приложение восстанавливает отсутствующие локальные файлы из S3.

В результате удалены очередь изменений, cursor paging, универсальный JSON transport,
legacy JDBC gateway и mock, а `SyncService` теперь координирует mirror reconciliation.

## Новая архитектура синхронизации

### Инвентарь mirror-таблиц

В `MirrorSyncTable` зарегистрированы:

- `vendor`
- `document`
- `declaration`
- `product`
- `transact`
- `experiment`
- `product_specification`
- `safety_stock`
- `experiment_entry`
- `experiment_reminder`
- `product_declaration`
- `composition`
- `batch`
- `batch_cost_price`
- `batch_movement`
- `reminder`
- `expense`
- `buy`
- `sale`
- `file`

Порядок в enum используется при применении данных:

- upsert выполняется от родительских сущностей к дочерним;
- delete выполняется в обратном порядке;
- это снижает риск нарушения локальных внешних ключей.

### Typed mirror rows и mapper-слой

Добавлен пакет `database.data.sync.mirror`, содержащий:

- typed-модели строк для всех синхронизируемых таблиц;
- mapper-ы корневых сущностей;
- mapper-ы дочерних сущностей и связей;
- mapper-ы транзакций;
- отдельные mapper-ы файлов и себестоимости партий;
- разрешение локальных display label и ссылок через `sync_id`;
- кодеки для преобразования typed rows в JDBC-параметры и обратно.

Локальные числовые `id` не передаются как межтабличные идентификаторы. При построении
snapshot они преобразуются в `sync_id`, а при pull удаленные ссылки снова разрешаются
в локальные Room `id`.

### Reconciliation planner

`MirrorReconciliationPlanner` сравнивает локальный и удаленный snapshot по каждой
таблице и каждому `sync_id`.

Правила:

- локальная строка отсутствует, удаленная активна: строка идет в pull;
- удаленная строка отсутствует: локальная строка идет в push;
- обе строки существуют: сравнивается их версия;
- локальная версия новее: push;
- удаленная версия новее: pull;
- версии равны: действие не требуется;
- удаленный tombstone без локальной строки повторно локально не применяется.

Версия строки определяется как максимальная логическая дата:

```text
deleted_at > updated_at ? deleted_at : updated_at
```

### Reconciliation service

`MirrorReconciliationService` реализует:

- расчет текущего количества локальных и удаленных расхождений;
- push локальных победителей;
- pull удаленных победителей;
- полную пересборку remote mirror из локальной базы;
- создание tombstone для удаленных remote-строк при disaster recovery.

Push и pull больше не зависят от очереди событий. Состояние определяется сравнением
двух актуальных снимков.

### YDB JDBC gateway

Добавлен `YdbJdbcMirrorSyncGateway` и новая конфигурация
`YdbMirrorJdbcConfig`.

Gateway:

- проверяет конфигурацию и доступность YDB;
- проверяет наличие typed mirror tables;
- создает отсутствующие mirror tables при проверке статуса или синхронизации;
- загружает remote snapshot;
- выполняет typed `UPSERT`;
- поддерживает optional root directory для mirror tables.

Новые параметры:

- `NOCOMBRO_YDB_JDBC_URL` / `nocombro.ydb.jdbcUrl`
- `NOCOMBRO_YDB_SA_FILE` / `nocombro.ydb.saFile`
- `NOCOMBRO_YDB_TOKEN` / `nocombro.ydb.token`
- `NOCOMBRO_YDB_MIRROR_ROOT` / `nocombro.ydb.mirrorRoot`

Если URL не задан, используется `NoopMirrorSyncRemoteGateway`, а UI показывает,
что remote mirror не настроен. Старого mock remote transport больше нет.

## Обработка удалений

### Почему понадобился журнал

Часть существующих репозиториев физически удаляет строки из Room. После такого
удаления обычный snapshot уже не может узнать, какая строка существовала и какой
payload нужно отправить в YDB.

Для этого добавлена таблица:

```text
mirror_deletion_journal
```

Она хранит:

- имя таблицы;
- `sync_id`;
- сериализованную typed mirror row;
- время удаления.

### Capture hard deletes

`MirrorDeletionJournalRepository`:

1. Снимает snapshot до операции.
2. Выполняет изменение в той же Room-транзакции.
3. Снимает snapshot после операции.
4. Находит исчезнувшие строки, включая cascade delete.
5. Превращает их в tombstone.
6. Сохраняет tombstone в журнале.

Если удаляется владелец файла, связанные записи `file` также удаляются локально и
фиксируются в журнале. Это не оставляет file metadata привязанными к уже удаленной
бизнес-сущности.

### Применение remote tombstone

При pull удаленные tombstone сначала сохраняются в локальный журнал, затем изменения
применяются к Room в корректном порядке зависимостей.

Журнал участвует в последующих local snapshot. Поэтому tombstone не теряется после
физического удаления строки из Room и может повторно участвовать в reconciliation.

## Изменения Room и миграции

Версия `NocombroDatabase` увеличена с `3` до `7`.

### Миграция 3 -> 4

`batch_cost_price` получил sync metadata:

- `sync_id`
- `updated_at`
- `deleted_at`

Для существующих строк первоначально генерируется UUID.

### Миграция 4 -> 5

Создается `mirror_deletion_journal`.

### Миграция 5 -> 6

`batch_cost_price.sync_id` приводится к `batch.sync_id`.

Это закрепляет инвариант:

```text
batch_cost_price.sync_id == batch_cost_price.batch_sync_id == batch.sync_id
```

Так себестоимость партии может надежно восстанавливаться на другом компьютере,
несмотря на различающиеся локальные `batch.id`.

### Миграция 6 -> 7

- удаляется локальная таблица `sync_change`;
- `sync_state` пересоздается без `device_id`;
- удаляется `last_remote_cursor`;
- сохраняются `last_pull_at` и `last_push_at`.

Добавлены Room schema snapshots для версий `4`, `5`, `6` и `7`.

## Удаленный DDL

Добавлен `database/ydb/mirror_sync_v1.sql`.

Он описывает все typed mirror tables и их поля. Для дат пока используется ISO-8601
в `Utf8`, чтобы сохранить текущее timezone-neutral поведение Kotlin-моделей.

Добавлен `database/ydb/drop_legacy_sync_tables.sql` для ручного удаления:

- `sync_push_log`;
- `reminder_email_source`;
- `experiment_reminder_email_source`.

Приложение этот cleanup SQL не запускает. Удаление legacy tables предполагается
только после обновления всех клиентов, проверки sync и disaster recovery.

## Удаленный legacy transport

Удалены классы старой event-based синхронизации:

- `SyncChangeEntity`
- `SyncChangeType`
- `SyncDao`
- `SyncEntityExportRepository`
- `SyncPayloadModels`
- `SyncQueueRepository`
- `SyncQueueStatus`
- `SyncRemoteApplyRepository`
- `SyncRemoteGateway`
- `SyncRunner`
- `YdbJdbcConfig`
- `YdbJdbcSyncGateway`
- `YdbSyncGatewayMock`

Также удалены:

- SQL для `sync_push_log`;
- тесты старого paging/payload transport;
- инструменты Doctor для поиска битых строк legacy sync log.

## Обновление SyncService

`SyncService` теперь зависит от:

- `SyncStateRepository`;
- `MirrorReconciliationService`;
- optional `RemoteFileBatchDownloadRepository`.

### Push

Push:

1. Проверяет статус mirror gateway.
2. Загружает local и remote snapshot.
3. Рассчитывает reconciliation plan.
4. Отправляет local winners.
5. Обновляет `last_push_at`.

### Pull

Pull:

1. Загружает local и remote snapshot.
2. Рассчитывает reconciliation plan.
3. Применяет remote winners в Room.
4. Обновляет `last_pull_at`.
5. При настроенном S3 скачивает отсутствующие локальные файлы.

### Статус

Вместо queue-specific полей используются:

- `pendingLocalChangesCount`;
- `remoteChangesCount`;
- `hasRemoteChanges`;
- `remoteSyncConfigured`;
- `remoteError`;
- даты последней проверки, push и pull.

Удалены:

- количество failed queue items;
- payload version;
- remote cursor.

## DI и запуск приложения

В database/root Koin wiring:

- удалена регистрация legacy queue, runner, exporter и apply repository;
- зарегистрированы snapshot/apply repositories;
- зарегистрирован planner и reconciliation service;
- зарегистрирован typed mirror gateway;
- `SyncService` собирается через новый набор зависимостей;
- `SyncStateRepository` использует выделенный `SyncStateDao`.

`initKoin` получил `databaseOverride`, чтобы smoke/integration тесты могли запускать
root graph на тестовой базе без подмены production-файла.

Инициализация базы больше не создает и не читает `device.id`: device-specific
identity не требуется snapshot-based протоколу.

## Изменения feature-репозиториев

### Общий mutable table toolkit

Legacy wrappers с enqueue в sync queue заменены на:

- `TransactionalCreateSingleItemRepository`;
- `TransactionalUpdateSingleLineRepository`;
- `TransactionalUpdateCollectionRepository`.

Новые wrappers оставляют в общей инфраструктуре:

- сравнение старого и нового состояния;
- подготовку объекта;
- валидацию;
- выполнение изменения в одной транзакции;
- optional перехват hard delete для tombstone journal.

Они больше не знают о конкретном remote transport.

### Формы

Формы vendor, document, declaration, expense, product и transaction переведены
с `SyncQueueRepository` на обычные транзакционные записи.

Для коллекций и сценариев с физическим удалением подключен
`MirrorDeletionJournalRepository`.

Особенно важные сценарии:

- composition;
- product declaration;
- safety stock;
- buy и связанный batch movement;
- sale и связанный batch movement;
- reminders;
- expenses;
- ingredients;
- каскадные удаления из transaction form.

### Experiments

Из `ExperimentsRepository` удален ручной enqueue для:

- experiment;
- experiment entry;
- experiment reminder.

Записи по-прежнему обновляют `updated_at`, а mirror sync обнаруживает изменения
через snapshot comparison.

### Batch cost price

`BatchCostPriceEntity` получил стабильный sync identity и timestamps.

Расчет себестоимости:

- обновляет существующую строку с новым `updated_at`;
- при создании использует `batch.sync_id`;
- сбрасывает `deleted_at`;
- сохраняет связь с партией без зависимости от локального числового ID.

## Файлы и S3

### File metadata в mirror

Таблица `file` включена в typed mirror sync. В YDB передаются metadata:

- владелец и его `sync_id`;
- display name;
- локальный path;
- remote object key;
- storage provider;
- timestamps и tombstone.

Сами бинарные данные продолжают храниться в S3.

### Восстановление после pull

После применения remote mirror приложение запускает пакетное восстановление
отсутствующих локальных копий из S3.

Результат загрузки включается в `SyncRunResult` и отображается в sync UI.

### Исправления S3 gateway

Обновлена работа `S3RemoteFileStorageGateway`, включая сценарии ошибок и повторного
получения объектов. Добавлены отдельные тесты gateway и batch download repository.

### Безопасная очистка S3

`RemoteFilesMaintenanceRepository` больше не считает локальную Room-таблицу
единственным источником истины для cleanup.

Новый алгоритм:

1. Проверить доступность mirror gateway.
2. Загрузить свежий snapshot mirror-таблицы `file`.
3. Выбрать активные `remote_object_key`.
4. Загрузить список объектов S3.
5. Считать orphan только объект, которого нет в активном mirror.
6. Непосредственно перед удалением повторно проверить актуальное состояние.

Batch delete удаляет только подтвержденные orphan-объекты. Это уменьшает риск
удалить файл, который отсутствует на текущем компьютере, но используется другой
синхронизированной установкой.

## Doctor

Из Doctor удален legacy-инструмент очистки битых строк универсального sync log,
поскольку такого transport больше нет.

S3 diagnostics теперь сравнивает три набора:

- локальные прикрепленные object keys;
- активные object keys из YDB mirror `file`;
- реальные object keys в S3.

В лог выводятся:

- количество ключей в каждом источнике;
- local-only;
- mirror-only;
- S3-only.

Remote orphan cleanup работает относительно mirror, а не только текущей Room-БД.

## UI синхронизации

Top bar и sync dropdown адаптированы к snapshot-based модели.

Теперь UI показывает:

- количество локальных изменений, которые победят при push;
- количество удаленных изменений, которые победят при pull;
- наличие remote changes;
- состояние конфигурации;
- время последней проверки;
- время последней синхронизации и pull;
- результат восстановления файлов;
- текущую ошибку gateway или reconciliation.

Удалены queue-specific индикаторы:

- failed queue items;
- pending queue badge;
- payload version;
- remote cursor.

Clipboard snapshot также переведен на новые поля.

## Cloud reminder email

Cloud-функция ежедневной рассылки больше не читает отдельные source tables.

Транзакционные напоминания загружаются через join:

```text
reminder -> transact
```

Напоминания экспериментов загружаются через join:

```text
experiment_reminder -> experiment
```

Учитываются tombstone обеих сторон связи.

В `.env.example`:

- удалены имена legacy reminder source tables;
- добавлен `YDB_MIRROR_ROOT`;
- сохранены отдельные таблицы recipients и delivery log.

`reminder_recipient` и `reminder_email_delivery` остаются состоянием cloud-функции
и не входят в desktop mirror transport.

## Навигация и root tests

Root wiring и smoke tests обновлены под новую сигнатуру sync-компонента и новый DI.

Изменения в main tab routing минимальны и связаны с обновлением test/navigation
assembly, а не с добавлением новой пользовательской вкладки.

## Добавленные тесты

### Room и миграции

- `DatabaseMigration5To6Test`
- `DatabaseMigration6To7Test`
- schema snapshots `4.json` - `7.json`

### Reconciliation и локальное применение

- `MirrorSyncTableTest`
- `MirrorReconciliationPlannerTest`
- `MirrorDeletionJournalRepositoryTest`
- `MirrorLocalApplyRepositoryTest`
- `SyncServiceMirrorStatusTest`

### YDB codecs и integration

- `YdbMirrorRowCodecTest`
- `YdbMirrorIntegrationTest`
- `YdbMirrorLocalAuditTest`
- `YdbMirrorSmokeCleanupTest`
- `YdbMirrorWorkingDatabaseRebuildTest`

### Disaster recovery

- `YdbMirrorDisasterRecoveryTest`
- `YdbMirrorFileRepairTest`

Эти сценарии проверяют восстановление пустой/рабочей Room-БД из mirror и возврат
файлов из S3.

### Ручная проверка второй установки

Добавлен `tools/run-device2.ps1`.

Скрипт:

- запускает приложение с отдельным каталогом `%APPDATA%`;
- не затрагивает основную локальную Room-БД;
- при необходимости полностью очищает тестовый каталог через `-Reset`;
- копирует локальные S3/YDB конфиги из основной установки;
- позволяет вручную проверять push/pull между двумя изолированными Room-БД.

### Files

- `RemoteFileBatchDownloadRepositoryTest`
- `S3RemoteFileStorageGatewayTest`
- `RemoteFilesMaintenanceRepositoryTest`

### Обновленные smoke tests

- `ExperimentsNavigationSmokeTest`
- `RootNocombroFlowSmokeTest`

`TestDatabaseFactory` обновлен для новой версии Room и нового набора миграций.

## Документация

`YDB_SYNC.md` полностью обновлен и теперь описывает:

- typed mirror architecture;
- список mirror tables;
- локальный deletion journal;
- runtime variables;
- поведение push/pull;
- S3 recovery;
- Doctor cleanup;
- ручное удаление legacy tables;
- команды обычных и YDB smoke tests;
- известное ограничение server-side version protection.

README cloud reminder pipeline обновлен под прямое чтение mirror tables.

## Порядок внедрения

Рекомендуемая последовательность выпуска:

1. Сделать резервную копию локальной Room-БД и текущих YDB tables.
2. Развернуть версию приложения с Room migrations до версии `7`.
3. Настроить `NOCOMBRO_YDB_JDBC_URL` и авторизацию.
4. При необходимости настроить `NOCOMBRO_YDB_MIRROR_ROOT`.
5. Проверить, что status видит все typed mirror tables.
6. Выполнить push на компьютере с эталонной полной локальной базой.
7. Выполнить pull на второй установке.
8. Сравнить local/mirror состояние через Doctor и тесты.
9. Проверить загрузку отсутствующих файлов из S3.
10. Проверить восстановление тестовой пустой Room-БД.
11. Обновить cloud reminder function на чтение mirror tables.
12. Только после обновления всех клиентов вручную выполнить
    `drop_legacy_sync_tables.sql`.

## Известные ограничения и риски

### Server-side compare-and-set

Текущий YDB `UPSERT` не защищен server-side условием версии.

Теоретический сценарий:

1. Два клиента загрузили один remote snapshot.
2. Первый отправил новую версию.
3. Второй позже отправил ранее рассчитанный, но уже устаревший snapshot.
4. Поздний `UPSERT` может перезаписать более новую строку.

Задача вынесена в issue `#131`.

### Стоимость полного snapshot

Каждая проверка статуса, push и pull загружает snapshots таблиц. Для текущего
небольшого объема это упрощает протокол и восстановление, но при значительном росте
данных может потребоваться инкрементальная оптимизация.

### Tombstone retention

Журнал удалений и remote tombstone пока не имеют автоматической политики очистки.
Удалять их можно только после определения безопасного retention window для всех
клиентов.

### Совместимость клиентов

Legacy и mirror clients не должны долго работать параллельно как равноправные
писатели: они используют разные transport models. Legacy YDB tables следует удалять
только после завершения миграции всех рабочих установок.

### Локальный path файла

File metadata содержит path, но фактическое восстановление должно опираться на S3
object key и локальную стратегию размещения. Путь с другого компьютера не следует
считать переносимым абсолютным адресом.

## Рекомендуемый порядок ревью

Изменения лучше смотреть не в алфавитном порядке из `git diff`, а по направлению
зависимостей: сначала модель данных и протокол, затем алгоритм, локальная интеграция,
feature-репозитории, UI и тесты.

### Шаг 1. Зафиксировать границы изменения

Сначала посмотреть общую статистику:

```powershell
git status --short
git diff --stat
git diff --name-status
```

После этого прочитать:

1. `YDB_SYNC.md`
2. `database/ydb/mirror_sync_v1.sql`
3. `database/ydb/drop_legacy_sync_tables.sql`

Что проверить:

- понятна ли целевая архитектура;
- совпадает ли список remote tables с бизнес-сущностями приложения;
- нет ли таблиц, которые случайно перестали синхронизироваться;
- действительно ли legacy cleanup выполняется только вручную;
- приемлемо ли известное ограничение с поздним `UPSERT` устаревшей версии.

На этом этапе не нужно погружаться в UI или формы. Цель — согласовать сам протокол.

### Шаг 2. Проверить Room schema и миграции

Смотреть:

1. `database/src/desktopMain/kotlin/ru/pavlig43/database/NocombroDatabase.kt`
2. `database/src/desktopMain/kotlin/ru/pavlig43/database/DatabaseMigration.kt`
3. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/batch/BatchCostPriceEntity.kt`
4. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/SyncStateEntity.kt`
5. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/SyncStateDao.kt`
6. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorDeletionJournalEntity.kt`
7. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorDeletionJournalDao.kt`
8. `database/schemas/ru.pavlig43.database.NocombroDatabase/4.json`
9. `database/schemas/ru.pavlig43.database.NocombroDatabase/5.json`
10. `database/schemas/ru.pavlig43.database.NocombroDatabase/6.json`
11. `database/schemas/ru.pavlig43.database.NocombroDatabase/7.json`

Что проверить:

- все новые entities зарегистрированы в Room;
- версия базы соответствует последней schema;
- цепочка миграций непрерывна: `3 -> 4 -> 5 -> 6 -> 7`;
- `batch_cost_price` получает правильный `sync_id`;
- миграция `5 -> 6` корректно связывает себестоимость с `batch.sync_id`;
- `sync_change` удаляется только после появления нового механизма tombstone;
- `sync_state` сохраняет даты предыдущих push/pull;
- индексы и внешние ключи совпадают со schema snapshots.

Это один из самых критичных этапов: ошибка здесь может повредить существующую
пользовательскую базу еще до запуска синхронизации.

### Шаг 3. Проверить mirror-контракты и полный список таблиц

Смотреть:

1. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorSyncContracts.kt`
2. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorSyncTable.kt`
3. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorSyncRows.kt`

Что проверить:

- remote gateway не протекает деталями JDBC в domain-слой;
- каждая DDL-таблица имеет соответствующую typed row;
- `tableName` совпадает с Room и YDB;
- `applyOrder` учитывает все связи parent/child;
- nullable-поля соответствуют фактическим сущностям;
- денежные значения, даты, enum и boolean не теряют тип или точность;
- `transact` намеренно используется вместо SQL-опасного имени `transaction`.

Полезная сверка:

```text
MirrorSyncTable <-> MirrorSyncRow <-> mirror_sync_v1.sql
```

У этих трех представлений должен быть одинаковый состав таблиц и полей.

### Шаг 4. Проверить mapper-ы Room <-> mirror

Смотреть:

1. `RootMirrorMappers.kt`
2. `ChildMirrorMappers.kt`
3. `RelationMirrorMappers.kt`
4. `TransactionMirrorMappers.kt`
5. `BatchCostPriceMirrorMapper.kt`
6. `FileMirrorMapper.kt`
7. `MirrorDisplayLabelResolver.kt`

Все файлы находятся в:

```text
database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/
```

Что проверить:

- локальные `id` не уходят в remote relations;
- каждая связь преобразуется в правильный `*_sync_id`;
- обратное применение умеет найти локального родителя;
- нет перепутанных owner type, transaction type или movement type;
- display name используется только как данные, а не как identity;
- file owner корректно разрешается для каждого `OwnerType`;
- `batch_cost_price.sync_id` совпадает с `batch_sync_id`;
- mapper не генерирует новый `sync_id` во время обычного чтения snapshot;
- `updated_at` и `deleted_at` сохраняются без неявного обновления.

Этот шаг удобнее делать таблица за таблицей, сверяясь с DDL из шага 1.

### Шаг 5. Проверить алгоритм выбора победителя

Смотреть:

1. `MirrorReconciliationPlanner.kt`
2. `MirrorReconciliationService.kt`

Что проверить:

- корректно обработаны четыре состояния: только local, только remote, обе версии,
  отсутствие обеих версий;
- tombstone участвует в сравнении по своей дате;
- равные версии не создают бесконечный push/pull;
- remote-only tombstone не пытается удалить уже отсутствующую локальную строку;
- push не применяет remote winners локально;
- pull не отправляет local winners;
- status считает оба направления расхождений;
- rebuild remote создает tombstone для remote-строк, отсутствующих локально;
- время завершения операции имеет понятную семантику.

Отдельно решить, устраивает ли product-решение:

```text
last write wins по timestamp без server-side compare-and-set
```

Если нет, это блокирующее архитектурное замечание, а не локальная правка.

### Шаг 6. Проверить журнал hard delete

Смотреть:

1. `MirrorDeletionJournalRepository.kt`
2. `MirrorLocalSnapshotRepository.kt`
3. `MirrorLocalApplyRepository.kt`
4. `MirrorEntityApplyRepository.kt`

Что проверить:

- snapshot до удаления и после удаления выполняется в одной транзакции;
- cascade delete действительно обнаруживается сравнением snapshots;
- удаленные строки сериализуются с корректным concrete row type;
- tombstone не теряется после физического удаления Room-строки;
- journal и активная строка с одинаковым `sync_id` объединяются по более новой версии;
- remote tombstone сначала сохраняется в journal;
- upsert идет parent -> child;
- delete идет child -> parent;
- повторный pull идемпотентен;
- удаление владельца захватывает связанные file metadata;
- ошибка в середине apply откатывает всю Room-транзакцию.

Это второй наиболее рискованный участок после миграций.

### Шаг 7. Проверить YDB gateway и кодеки

Смотреть:

1. `YdbMirrorJdbcConfig.kt`
2. `YdbJdbcMirrorSyncGateway.kt`
3. `YdbMirrorRowCodec.kt`
4. `YdbMirrorRelationCodecs.kt`
5. `NoopMirrorSyncRemoteGateway.kt`

Что проверить:

- environment variables и JVM properties имеют ожидаемый приоритет;
- default service-account path не попадает в репозиторий;
- `mirrorRoot` корректно нормализуется;
- table creation совпадает с checked-in DDL;
- запросы экранируют пути таблиц;
- nullable JDBC-параметры передаются корректно;
- все row types поддерживаются в обе стороны;
- ошибки авторизации и сети возвращаются в status, а не маскируются;
- отсутствие конфигурации отличается от недоступного настроенного remote;
- gateway не удаляет legacy tables автоматически.

После чтения gateway еще раз сравнить его DDL с:

```text
database/ydb/mirror_sync_v1.sql
```

### Шаг 8. Проверить SyncService и DI

Смотреть:

1. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/SyncService.kt`
2. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/SyncStateRepository.kt`
3. `database/src/desktopMain/kotlin/ru/pavlig43/database/DI.kt`
4. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/DatabaseModule.kt`
5. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/InitKoin.kt`

Что проверить:

- все legacy sync dependencies действительно удалены из graph;
- production без YDB URL стартует через noop gateway;
- настроенный YDB gateway создается с правильной конфигурацией;
- `SyncService.syncOnce()` останавливается при ошибке push;
- pull запускает восстановление файлов только после успешного local apply;
- ошибка загрузки файлов не скрывает уже примененный pull;
- даты `last_push_at` и `last_pull_at` обновляются только после успешной операции;
- `databaseOverride` не меняет production initialization order;
- инициализация `sync_state` не создает гонку с первым status request.

Здесь полезно искать оставшиеся ссылки на legacy API:

```powershell
rg "SyncQueueRepository|SyncRunner|SyncRemoteGateway|lastRemoteCursor|sync_change" .
```

Ожидаемыми результатами должны оставаться только документация, миграции или явно
legacy cleanup-контекст.

### Шаг 9. Проверить feature write paths

Сначала посмотреть общие wrappers:

1. `features/table/mutable/src/desktopMain/kotlin/ru/pavlig43/mutable/api/singleLine/data/TransactionalCreateSingleItemRepository.kt`
2. `features/table/mutable/src/desktopMain/kotlin/ru/pavlig43/mutable/api/singleLine/data/TransactionalUpdateSingleLineRepository.kt`
3. `features/table/mutable/src/desktopMain/kotlin/ru/pavlig43/mutable/api/multiLine/data/TransactionalUpdateCollectionRepository.kt`

Затем DI и репозитории форм:

1. `features/form/vendor/.../VendorFormModule.kt`
2. `features/form/document/.../DocumentFormModule.kt`
3. `features/form/declaration/.../DeclarationFormModule.kt`
4. `features/form/expense/.../ExpenseFormModule.kt`
5. `features/form/product/.../ProductFormModule.kt`
6. `features/form/transaction/.../CreateTransactionFormModule.kt`
7. `features/experiments/.../ExperimentsRepository.kt`

Что проверить:

- удаление enqueue не изменило бизнес-валидацию;
- create/update по-прежнему выполняются транзакционно;
- `updated_at` обновляется для каждой измененной строки;
- unchanged item не получает лишний timestamp;
- коллекции правильно различают create, update и delete;
- все физические удаления обернуты в capture hard deletes;
- сложные buy/sale операции захватывают удаление movement;
- safety stock корректно создает tombstone при переходе к нулевым значениям;
- каскадные удаления формы не обходят journal;
- расчет batch cost сохраняет прежнюю бизнес-формулу.

Удобно ревьюить не все формы подряд, а по типу операции:

1. простой create/update;
2. коллекции;
3. физические удаления;
4. каскадные transaction-сценарии.

### Шаг 10. Проверить контур файлов и S3

Смотреть:

1. `database/.../files/remote/S3RemoteFileStorageGateway.kt`
2. `database/.../files/remote/RemoteFileBatchDownloadRepository.kt`
3. `features/files/.../FilesRepository.kt`
4. `features/files/.../RemoteFilesMaintenanceRepository.kt`
5. `features/form/product/.../ProductSpecificationPdfRepository.kt`

Что проверить:

- binary upload/delete не смешан с mirror metadata transaction;
- pull скачивает только отсутствующие локальные файлы;
- повторный download безопасен;
- S3 cleanup сравнивает S3 с активным remote mirror, а не только с текущей Room;
- перед batch delete выполняется повторная проверка;
- tombstone file metadata исключается из active keys;
- ошибка mirror запрещает cleanup;
- ошибка S3 не превращается в пустой список и не разрешает массовое удаление;
- удаление локальной сущности не обязано немедленно удалять S3 object до общего sync.

Это критично с точки зрения необратимой потери пользовательских файлов.

### Шаг 11. Проверить Doctor и sync UI

Смотреть:

1. `features/doctor/src/desktopMain/kotlin/ru/pavlig43/doctor/api/DoctorDependencies.kt`
2. `features/doctor/src/desktopMain/kotlin/ru/pavlig43/doctor/api/component/DoctorComponent.kt`
3. `features/doctor/src/desktopMain/kotlin/ru/pavlig43/doctor/api/ui/DoctorScreen.kt`
4. `features/doctor/src/desktopMain/kotlin/ru/pavlig43/doctor/internal/component/DoctorModels.kt`
5. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/SyncComponent.kt`
6. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/topbar/ui/NocombroAppBar.kt`

Что проверить:

- UI больше не обещает queue semantics;
- local/remote change counts подписаны однозначно;
- badge не скрывает критическую remote error;
- кнопки недоступны во время выполняющейся операции;
- Doctor cleanup нельзя запустить без доступных mirror и S3;
- удаленный список обновляется перед опасным действием;
- диагностический clipboard не содержит устаревшие cursor/payload поля;
- удаление legacy Doctor tool не оставило мертвый route или enum.

UI стоит ревьюить после core, иначе названия счетчиков трудно оценить по смыслу.

### Шаг 12. Проверить cloud reminder pipeline

Смотреть:

1. `cloud/reminder-email/functions/send_daily_emails.py`
2. `cloud/reminder-email/functions/.env.example`
3. `cloud/reminder-email/ydb/001_email_reminder_schema.sql`
4. `cloud/reminder-email/README.md`

Что проверить:

- joins используют правильные `*_sync_id`;
- tombstone фильтруются у reminder и родителя;
- `YDB_MIRROR_ROOT` формирует тот же путь, что desktop gateway;
- имена колонок совпадают с mirror DDL;
- recipient и delivery tables остаются независимыми от desktop sync;
- deployment больше не требует legacy source tables.

### Шаг 13. Читать тесты в том же порядке, что production-код

Рекомендуемый порядок:

1. `DatabaseMigration5To6Test.kt`
2. `DatabaseMigration6To7Test.kt`
3. `MirrorSyncTableTest.kt`
4. `MirrorReconciliationPlannerTest.kt`
5. `MirrorDeletionJournalRepositoryTest.kt`
6. `MirrorLocalApplyRepositoryTest.kt`
7. `YdbMirrorRowCodecTest.kt`
8. `SyncServiceMirrorStatusTest.kt`
9. `RemoteFileBatchDownloadRepositoryTest.kt`
10. `RemoteFilesMaintenanceRepositoryTest.kt`
11. `YdbMirrorIntegrationTest.kt`
12. `YdbMirrorLocalAuditTest.kt`
13. `YdbMirrorWorkingDatabaseRebuildTest.kt`
14. `YdbMirrorFileRepairTest.kt`
15. `YdbMirrorDisasterRecoveryTest.kt`
16. root navigation/smoke tests.

Что искать в тестах:

- конфликт local-newer и remote-newer;
- равные версии;
- tombstone новее активной строки;
- каскадное удаление;
- повторное применение одного snapshot;
- разные локальные `id` при одинаковых `sync_id`;
- пустая Room-БД;
- неполная локальная база;
- S3 object существует, локального файла нет;
- cleanup не удаляет active mirror object;
- миграция реальных старых данных, а не только пустой schema.

### Шаг 14. Финальный поиск и проверка удаления legacy-кода

После смыслового ревью выполнить:

```powershell
rg "SyncQueueRepository|SyncChangeEntity|SyncRunner|YdbJdbcSyncGateway" .
rg "last_remote_cursor|lastRemoteCursor|payloadVersion|sync_push_log" .
rg "reminder_email_source|experiment_reminder_email_source" .
```

Затем отдельно посмотреть удаления:

```powershell
git diff --diff-filter=D --name-status
```

Что проверить:

- удаленный класс не имел несинхронизационной ответственности;
- старые tests удалены потому, что transport исчез, а не потому, что сценарий стал
  неудобно проверять;
- нет документации или cloud deployment, все еще завязанных на legacy tables;
- нет случайно удаленных пользовательских инструментов Doctor.

### Шаг 15. Запуск проверок

Минимальный порядок запуска:

```powershell
.\gradlew :database:desktopTest
.\gradlew :features:files:desktopTest
.\gradlew :rootnocombro:desktopTest
.\gradlew detektAll
```

После локальных тестов:

1. YDB integration smoke.
2. Local audit между Room и mirror.
3. File repair.
4. Working database rebuild.
5. Полный disaster recovery.
6. Ручной запуск desktop-приложения.

При ручной проверке пройти сценарий на двух локальных базах:

1. Создать сущность на компьютере A.
2. Push с A.
3. Pull на B.
4. Изменить сущность на B.
5. Pull/push в обоих направлениях.
6. Удалить дочернюю сущность.
7. Удалить родительскую сущность с cascade.
8. Проверить tombstone и отсутствие воскрешения.
9. Прикрепить файл и проверить восстановление из S3.
10. Убедиться, что Doctor не предлагает удалить активный объект.

### Короткий маршрут для первого прохода

Если сначала нужен обзор без детального чтения каждого mapper-а:

1. `YDB_SYNC.md`
2. `mirror_sync_v1.sql`
3. `NocombroDatabase.kt`
4. `DatabaseMigration.kt`
5. `MirrorSyncTable.kt`
6. `MirrorReconciliationPlanner.kt`
7. `MirrorDeletionJournalRepository.kt`
8. `MirrorLocalApplyRepository.kt`
9. `YdbJdbcMirrorSyncGateway.kt`
10. `SyncService.kt`
11. `DatabaseModule.kt`
12. `CreateTransactionFormModule.kt`
13. `RemoteFilesMaintenanceRepository.kt`
14. `SyncComponent.kt`
15. `MirrorReconciliationPlannerTest.kt`
16. `YdbMirrorDisasterRecoveryTest.kt`

После этого уже имеет смысл возвращаться к mapper-ам и отдельным feature-модулям.

## Проверки перед коммитом

Для локальной проверки рекомендуется выполнить:

```powershell
.\gradlew :database:desktopTest
.\gradlew :features:files:desktopTest
.\gradlew :rootnocombro:desktopTest
.\gradlew detektAll
```

Для реального YDB smoke:

```powershell
$env:NOCOMBRO_YDB_SMOKE = "true"
.\gradlew :database:desktopTest --tests "ru.pavlig43.database.YdbMirrorIntegrationTest" --no-parallel
```

Для disaster recovery:

```powershell
$env:NOCOMBRO_YDB_DISASTER_RECOVERY = "true"
.\gradlew :database:desktopTest --tests "ru.pavlig43.database.YdbMirrorDisasterRecoveryTest" --no-parallel
```

Важно: этот отчет перечисляет добавленные и измененные тесты, но сам по себе не
утверждает, что реальные YDB smoke/disaster recovery тесты уже успешно выполнены в
текущем рабочем дереве.

Перед созданием коммита успешно выполнены:

```powershell
.\gradlew.bat :database:desktopTest :features:files:desktopTest :rootnocombro:desktopTest --no-parallel
.\gradlew.bat detektAll --no-parallel
```

`detektAll` завершился с `BUILD SUCCESSFUL`. В выводе остались неблокирующие
замечания конфигурации Detekt, в основном `MagicNumber` в JDBC row codecs и
`ReturnCount` в `SyncService`.

## Возможное описание будущего коммита

### Заголовок

```text
feat(sync): replace legacy queue transport with YDB mirror reconciliation
```

### Тело

```text
- add typed YDB mirror tables and JDBC row codecs
- reconcile Room and YDB snapshots by sync_id and timestamps
- preserve hard deletes through a local tombstone journal
- remove sync_change, cursor paging and legacy JSON payload transport
- migrate Room schema from version 3 to 7
- restore missing files from S3 after mirror pull
- make S3 orphan cleanup use active mirror file metadata
- migrate form repositories away from sync queue wrappers
- update sync UI, Doctor diagnostics and cloud reminder queries
- add migration, reconciliation, YDB, disaster recovery and file tests
```

## Итог

Изменения переводят проект от event queue синхронизации к модели удаленного typed
зеркала бизнес-таблиц. Новая реализация уменьшает количество transport-specific
кода в feature-модулях, поддерживает восстановление базы из YDB, синхронизирует file
metadata, восстанавливает бинарные файлы из S3 и делает remote cleanup зависимым от
общего mirror-состояния, а не от одной локальной установки.
