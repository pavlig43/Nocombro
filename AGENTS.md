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
```

Для локальной проверки лучше по возможности запускать только затронутый модуль.

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
