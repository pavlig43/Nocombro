# AGENTS.md

## Цель файла

Этот файл нужен как практическая памятка для быстрых задач по проекту.
Он короче и прикладнее, чем `PROJECT_OVERVIEW.md`.

Если задача локальная, сначала читать этот файл, а потом открывать только нужные модули.

## С чего почти всегда начинать

Для большинства задач достаточно открыть:

1. `settings.gradle.kts`
2. `app/desktopApp/src/desktopMain/kotlin/ru/pavlig43/nocombro/Main.kt`
3. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/RootNocombroComponent.kt`
4. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt`
5. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`
6. нужный `features/...` модуль

## Быстрая карта ответственности

### `app/desktopApp`

Desktop entrypoint, окно приложения, global keyboard/back handling, запуск root UI.

### `rootnocombro`

Главный orchestration-модуль:

- root component
- tab/drawer navigation
- связывание feature-компонентов
- root-level Koin wiring
- settings/theme hookup

Если непонятно, почему экран не открывается или не подключается, искать сначала здесь.

### `core`

Общие интерфейсы, tab abstractions, coroutine helpers и модели, которые используют разные фичи.

### `coreui`

Общий UI toolkit и утилиты уровня Compose/UI interaction.

### `database`

Room/SQLite, схемы и data layer infrastructure.

### `datastore`

Настройки и локально сохраняемые preferences.

### `features/*`

Изолированные бизнес-фичи. Обычно изменение конкретного экрана живет здесь, а `rootnocombro` только подключает его в приложение.

## Новые крупные зоны после agent/skills setup

Для этих зон сначала использовать соответствующий repo-local skill в `.codex/skills`, а не сканировать проект целиком:

- `nocombro-sync` - YDB mirror sync, Room sync metadata, tombstones, S3 recovery, sync UI.
- `nocombro-doctor` - Doctor diagnostics, storage overview, file cleanup, S3 orphan cleanup.
- `nocombro-testing` - smoke tests, real-data tests, test kits, YDB smoke/disaster checks.
- `nocombro-experiments` - experiments journal, entries, reminders, files, cloud reminder email.
- `nocombro-labels` - thermal labels, PPTX templates, label dialog/generator, related PDF output.

Если задача касается sync или file cleanup, сначала читать `YDB_SYNC.md`, потом уже код. Для sync/file cleanup нельзя опираться только на локальную Room-БД: S3 cleanup должен сверяться с активной remote mirror `file` metadata.

## Как ходить в удалённую YDB

Если нужно проверить удалённые mirror-таблицы, не угадывать способ доступа заново.

1. Убедиться у пользователя, что синхронизация уже завершена, если он сам пишет, что ещё синхронизирует.
2. Локальная БД: `%APPDATA%\Nocombro\nocombro.db`; читать через `sqlite3`, он видит актуальный WAL.
3. YDB JDBC URL брать из `tools/run-device2.ps1`, если env `NOCOMBRO_YDB_JDBC_URL` не задан:

```powershell
jdbc:ydb:grpcs://ydb.serverless.yandexcloud.net:2135/?database=/ru-central1/b1g87p6oufggn8merjua/etn8eb6ujifrk8lp7b73
```

4. Service-account key по умолчанию: `%APPDATA%\Nocombro\ydb-sa-key.json`. Не печатать его содержимое и не вставлять в ответ.
5. Если `ydb` CLI и Python-пакет `ydb` не установлены, идти через Java/JDBC: драйвер уже есть в Gradle cache как `tech.ydb.jdbc:ydb-jdbc-driver`. Не тащить весь Compose/Room classpath; собрать короткий classpath только из YDB/JDBC зависимостей.
6. Для писем по экспериментам проверять `experiment_reminder` + `experiment`, а не общую таблицу `reminder`.
7. Журнал отправок писем: `reminder_email_delivery`; получатели: `reminder_recipient`.
8. Для причин дублей смотреть `sync_id`, `updated_at`, `deleted_at`, `reminder_date_time`; активная строка имеет `deleted_at IS NULL`.

## Типовые маршруты изменений

### Добавить новый экран/таб

Обычно цепочка такая:

1. создать или обновить feature-модуль
2. подключить модуль в `rootnocombro/build.gradle.kts`
3. зарегистрировать dependencies в `rootnocombro/internal/di/RootNocombroModule.kt`
4. добавить config в `MainTabConfig.kt`
5. добавить child в `MainTabChild.kt`
6. подключить создание/navigation в `MainTabNavigationComponent.kt`
7. при необходимости отрисовать новый пункт в drawer/tab UI

### Изменить существующую форму

Сначала искать соответствующий модуль в:

- `features/form/document`
- `features/form/product`
- `features/form/vendor`
- `features/form/declaration`
- `features/form/transaction`
- `features/form/expense`

Если правка касается открытия формы или передачи `id`, дополнительно смотреть `MainTabConfig.kt` и root navigation.

### Изменить аналитику/таблицы/storage

Открывать сначала соответствующий feature:

- `features/analytic/main`
- `features/analytic/profitability`
- `features/table/*`
- `features/storage`

Если проблема не в логике экрана, а в том, как он открывается, возвращаться в `rootnocombro/internal/navigation`.

### Изменить sync/YDB mirror или восстановление файлов

Сначала читать:

1. `YDB_SYNC.md`
2. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/*`
3. `database/ydb/mirror_sync_v1.sql`
4. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/SyncComponent.kt`

Помнить:

- legacy queue transport удален;
- ключевые инварианты: `sync_id`, `updated_at`, `deleted_at`, tombstones;
- `database/ydb/drop_legacy_sync_tables.sql` запускается только вручную оператором, не приложением.

### Изменить Doctor/cleanup diagnostics

Смотреть:

- `features/doctor`
- `features/files/api/RemoteFilesMaintenanceRepository.kt`
- `database/src/desktopMain/kotlin/ru/pavlig43/database/data/files/remote`
- `rootnocombro/api/component/SyncComponent.kt`, если диагностика пересекается с sync status.

Опасные действия cleanup должны блокироваться при недоступном mirror или S3.

### Изменить experiments

Смотреть:

- `features/experiments`
- `database/src/desktopMain/kotlin/ru/pavlig43/database/data/experiment`
- `cloud/reminder-email`, если задача касается email reminders
- root navigation/DI, если экран не открывается.

### Изменить thermal labels или шаблоны

Смотреть:

- `features/label/thermal`
- `features/label/thermal/src/desktopMain/resources/templates`
- `docs/templates`
- product specification PDF code только если задача пересекается с PDF/specification output.

### Изменить тему или визуальную основу

Смотреть:

- `theme`
- `coreui`
- `rootnocombro/api/ui/App.kt`

### Изменить настройки приложения

Смотреть:

- `datastore`
- `rootnocombro/api/component/SettingsComponent.kt`
- `rootnocombro/api/ui/App.kt`

### Изменить DI

Проверять по порядку:

1. `rootnocombro/internal/di/InitKoin.kt`
2. `rootnocombro/internal/di/RootNocombroModule.kt`
3. `rootnocombro/internal/di/ModuleFactory.kt`
4. feature dependencies class в нужном модуле

## Навигация: что помнить

В проекте root navigation и main-tab navigation разделены.

- `RootNocombroComponent.kt` переключает верхний уровень (`Sign` / `Tabs`)
- `MainTabConfig.kt` описывает маршруты внутри основного приложения
- `MainTabChild.kt` связывает маршрут с типом компонента

Если экран "есть, но не открывается", чаще всего проблема именно в одной из этих трех точек.

## DI: что помнить

Feature-модуль обычно не достаточно просто добавить в Gradle.
Часто нужно еще:

- зарегистрировать dependencies class
- убедиться, что root scope умеет его создать
- проверить, что нужные репозитории/DAO уже видны из Koin

Если компиляция падает на `scope.get()` или не хватает зависимости, смотреть сначала wiring, а не UI.

## Команды, которые полезны почти всегда

```powershell
.\gradlew :app:desktopApp:run
.\gradlew build
.\gradlew detektAll
.\gradlew smokeDesktop
```

Для локальной проверки лучше по возможности запускать только затронутый модуль.

Для smoke/real-data проверок читать `test/docs/TEST_RUNBOOK.md`. Для проверки второго локального устройства и sync-сценариев смотреть `tools/run-device2.ps1`; он использует отдельный app-data root.

## Как смотреть исходники библиотек

Если нужно быстро найти исходник зависимости из Gradle cache, использовать:

```powershell
.\tools\find-gradle-source.ps1 -ClassName ua.wwind.table.ColumnSpec
```

С выводом содержимого:

```powershell
.\tools\find-gradle-source.ps1 -ClassName ua.wwind.table.ColumnSpec -ShowContent
```

Если нужно сузить поиск, можно добавить `-Group` и `-Artifact`, например:

```powershell
.\tools\find-gradle-source.ps1 -ClassName ua.wwind.table.ColumnSpec -Group ua.wwind.table-kmp -Artifact table-core-jvm
```

Скрипт сначала предпочитает `sources.jar`, а если его нет, ищет в обычных `jar`.

## Полезные рабочие предположения

- Это desktop-first конфигурация, даже если часть модулей оформлена как multiplatform.
- `rootnocombro` - главный вход для понимания того, как фичи собираются вместе.
- При UI-задачах сначала проверять navigation/wiring, потом уже детали feature-модуля.
- При data-задачах сначала проверять `database`/`datastore`, потом feature.

## Предпочтения по модели и делегированию

- Пользователь сам выбирает, разрешено ли использовать `sub-agent`'ов в текущей работе.
- Если использование `sub-agent`'ов разрешено, по умолчанию выбирать более дешевый путь, если это не дает заметной просадки по качеству.
- Более сильную модель или более дорогой режим использовать только когда ожидается явный выигрыш по качеству, снижению риска или работе с большим/сложным контекстом.
- `Sub-agent`'ов подключать только для хорошо делимых или параллелимых подзадач, а финальную проверку и интеграцию держать на основной сессии.
- Когда в задаче используются `sub-agent`'ы, явно сообщать об этом пользователю в промежуточных апдейтах.

## Чего не делать вслепую

- Не менять root navigation без проверки `MainTabConfig` и `MainTabChild` вместе.
- Не считать, что одного добавления зависимости в `build.gradle.kts` достаточно для подключения новой фичи.
- Не искать проблему запуска только в UI: часто причина в Koin wiring или root component assembly.

## Когда нужен `PROJECT_OVERVIEW.md`

Открывать `PROJECT_OVERVIEW.md`, если нужно:

- быстро понять весь ландшафт проекта
- вспомнить назначение модулей
- посмотреть основные технологии и команды
- войти в проект после долгого перерыва
