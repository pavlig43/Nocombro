# AGENTS.md

## Цель файла

Этот файл нужен как практическая памятка для быстрых задач по проекту.
Он короче и прикладнее, чем `PROJECT_OVERVIEW.md`.

Если задача локальная, сначала читать этот файл, а потом открывать только нужные модули.

## Android Studio semantic tools

Если Android CLI установлен и `android studio check` показывает READY для текущего проекта, используй Android Studio как IDE-сервис.

Перед Android-навигацией по коду сначала запускай:

```powershell
android studio check
```

На Windows захватывай `stderr`, если вывод пустой или выглядит как сбой:

```powershell
android studio check 2>&1 | Out-String
```

Java warning в `stderr` может дать non-zero exit code, но в том же выводе
всё равно может быть `READY`.

Если `android studio check` показывает проект как `READY`, бери имя проекта из
колонки `Projects` и передавай его в semantic-команды через `--project`.
Не полагайся на cwd: CLI может принять путь рабочей папки за имя проекта.
Для этого проекта пример:

```powershell
android studio find-declaration --short --project Nocombro SYMBOL
```

Для Kotlin/Android символов НЕ используй `rg`, `grep` или текстовый поиск первым способом.

Для поиска объявления символа используй:

```powershell
android studio find-declaration --short SYMBOL
```

Для поиска использований символа используй:

```powershell
android studio find-usages --short SYMBOL
```

Если символ неоднозначный, используй контекстный файл:

```powershell
android studio find-declaration --short --context-file=PATH_TO_FILE.kt SYMBOL
```

Для проверки конкретного Kotlin/Java файла через инспекции Android Studio используй:

```powershell
android studio analyze-file PATH_TO_FILE.kt
```

Для открытия файла в Android Studio используй:

```powershell
android studio open-file PATH_TO_FILE.kt
```

Для Compose Preview используй:

```powershell
android studio render-compose-preview --output-image-file=preview.png --print-semantics PATH_TO_FILE.kt PreviewFunctionName
```

Для проверки версий зависимостей используй:

```powershell
android studio version-lookup agp kotlin gradle compose
```

`rg`/`grep` разрешены только если:

* `android studio check` не READY;
* Android Studio закрыта;
* нужно найти обычный текст, строку, TODO, resource name или комментарий;
* Android Studio semantic command не нашёл результат.

Gradle-скрипты остаются основной проверкой сборки. Android Studio semantic tools нужны для навигации, инспекций, preview и анализа Android-кода.

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
3. YDB JDBC URL брать из `tools/run-device2.ps1`, если env `NOCOMBRO_YDB_JDBC_URL` не задан. Для CLI разделить его на endpoint и database:

```powershell
jdbc:ydb:grpcs://ydb.serverless.yandexcloud.net:2135/?database=/ru-central1/b1g87p6oufggn8merjua/etn8eb6ujifrk8lp7b73
```

```powershell
$endpoint = "grpcs://ydb.serverless.yandexcloud.net:2135"
$database = "/ru-central1/b1g87p6oufggn8merjua/etn8eb6ujifrk8lp7b73"
```

4. Service-account key по умолчанию: `%APPDATA%\Nocombro\ydb-sa-key.json`. Не печатать его содержимое и не вставлять в ответ.
5. На Windows сперва проверить CLI:

```powershell
Get-Command ydb -ErrorAction SilentlyContinue
Test-Path "$HOME\ydb\bin\ydb.exe"
```

Если `ydb` не найден, поставить официальным скриптом:

```powershell
Invoke-WebRequest -Uri "https://install.ydb.tech/cli-windows" -OutFile "$env:TEMP\install-ydb-cli-windows.ps1"
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& "$env:TEMP\install-ydb-cli-windows.ps1"
```

Скрипт кладёт бинарник в `$HOME\ydb\bin\ydb.exe`. Если он завис на вопросе `Add ydb installation dir to your PATH? [Y/n]`, не ждать: бинарник уже скачан, путь добавить вручную.

```powershell
$bin = Join-Path $HOME "ydb\bin"
$userPath = (Get-Item -Path "HKCU:\Environment").GetValue("Path", "", "DoNotExpandEnvironmentNames")
$parts = @()
if ($userPath) { $parts = $userPath -split ";" | Where-Object { $_ } }
if ($parts -notcontains $bin) {
    [Environment]::SetEnvironmentVariable("Path", (($parts + $bin) -join ";"), [System.EnvironmentVariableTarget]::User)
}
$env:Path = "$env:Path;$bin"
```

В уже запущенном Codex или PowerShell короткое имя `ydb` может ещё не работать из-за старого `PATH`. В этом случае брать полный путь:

```powershell
$ydb = Join-Path $HOME "ydb\bin\ydb.exe"
```

6. Быстрая read-only проверка подключения:

```powershell
$ydb = Join-Path $HOME "ydb\bin\ydb.exe"
$endpoint = "grpcs://ydb.serverless.yandexcloud.net:2135"
$database = "/ru-central1/b1g87p6oufggn8merjua/etn8eb6ujifrk8lp7b73"
$sa = Join-Path $env:APPDATA "Nocombro\ydb-sa-key.json"
& $ydb --endpoint $endpoint --database $database --sa-key-file $sa scheme ls
```

7. Пример read-only SQL:

```powershell
& $ydb --endpoint $endpoint --database $database --sa-key-file $sa sql --format tsv --script 'SELECT COUNT(*) AS rows_total FROM `product`;'
```

В PowerShell обратную кавычку в имени таблицы лучше собирать отдельно, если имя таблицы хранится в переменной:

```powershell
$bt = [char]96
$table = "product"
$query = "SELECT COUNT(*) AS rows_total FROM $bt$table$bt;"
& $ydb --endpoint $endpoint --database $database --sa-key-file $sa sql --format tsv --script $query
```

Если подряд запускать много `ydb sql`, YDB/IAM может вернуть:

```text
Status: UNAVAILABLE
Error: Too many concurrent requests
Error: Unable to resolve token, code: 200801
```

Обход: делать запросы медленнее (`Start-Sleep -Seconds 2..5` между вызовами) или собирать меньше отдельных CLI-вызовов. Для массовых проверок лучше один скрипт/запрос, а не десятки быстрых запусков CLI.

8. Если `ydb` CLI не удалось поставить и Python-пакет `ydb` тоже не установлен, идти через Java/JDBC: драйвер уже есть в Gradle cache как `tech.ydb.jdbc:ydb-jdbc-driver`. Не тащить весь Compose/Room classpath; собрать короткий classpath только из YDB/JDBC зависимостей.
9. Для писем по экспериментам проверять `experiment_reminder` + `experiment`, а не общую таблицу `reminder`.
10. Журнал отправок писем: `reminder_email_delivery`; получатели: `reminder_recipient`.
11. Для причин дублей смотреть `sync_id`, `updated_at`, `deleted_at`, `reminder_date_time`; активная строка имеет `deleted_at IS NULL`.

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
