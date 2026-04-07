# Nocombro Project Overview

## Что это

`Nocombro` - модульное desktop-приложение на Kotlin Multiplatform + Compose Multiplatform.
Сейчас фактическая целевая платформа в проекте - `desktop` JVM.

По структуре это бизнес-приложение с экранами/фичами для:

- таблиц и списков
- форм документов и справочников
- аналитики
- уведомлений
- хранилища/перемещений
- авторизации

## Главные точки входа

- Desktop entrypoint: `app/desktopApp/src/desktopMain/kotlin/ru/pavlig43/nocombro/Main.kt`
- Корневой Compose UI: `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/ui/App.kt`
- Инициализация DI: `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/InitKoin.kt`

Что происходит при старте:

1. В `Main.kt` поднимаются `Koin`, `FileKit`, `Decompose` lifecycle/back handling.
2. Создается `RootNocombroComponent`.
3. Открывается Compose `Window`.
4. Корневой UI рендерится через `App(rootNocombroComponent)`.

## Архитектурная карта

### `app/desktopApp`

Desktop launcher и настройка Compose Desktop application.

### `rootnocombro`

Главный composition root приложения:

- собирает зависимости фич
- конфигурирует root navigation
- хранит корневые компоненты и экран
- связывает drawer/tab navigation и app bar

Если нужно понять, как экран попадает в приложение, почти всегда начинать стоит отсюда.

### `core`

Базовые абстракции и утилиты, которые переиспользуются в feature-модулях:

- табы
- coroutine helpers
- модели общего назначения
- общие интерфейсы компонентов

### `coreui`

Общие UI-утилиты и переиспользуемый UI-слой.
По `Main.kt` видно, что здесь как минимум лежит обработка клавиатуры (`KeyEventHandler`).

### `theme`

Глобальная тема приложения (`NocombroTheme`).

### `database`

Модуль данных на `Room`/`SQLite bundled`.
Также здесь подключена генерация schema docs.

### `datastore`

Локальные настройки и persisted preferences через `DataStore`.

### `datetime`

Вспомогательный модуль для работы с датой/временем.

### `features/*`

Основная бизнес-логика разбита на отдельные feature-модули.
По `settings.gradle.kts` и `rootnocombro/build.gradle.kts` сейчас подключены:

- `features/sign/*` - auth/sign flow
- `features/form/*` - формы сущностей: document, product, vendor, declaration, transaction, expense
- `features/table/*` - табличные экраны
- `features/analytic/*` - аналитика и profitability
- `features/manageitem/*` - операции обновления/инициализации данных
- `features/notification`
- `features/files`
- `features/storage`
- `features/sampletable`

## Навигация

Навигация строится вокруг `RootNocombroComponent` и набора child-компонентов.

Файл:

- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`

По нему быстро видно набор основных экранов:

- notifications
- sample table
- storage
- analytic main
- profitability
- batch movement
- immutable table
- item forms: document/product/vendor/declaration/transaction/expense

Если нужно найти конкретный экран, удобно идти от `MainTabChild` и связанных config/component файлов в `rootnocombro/internal/navigation`.

## DI и зависимости

DI построен на `Koin`.

Базовый сценарий:

- `initKoin()` в `rootnocombro` стартует контейнер
- подключаются database/data store модули
- создается `RootDependencies`
- feature dependencies собираются в root-модуле

Если что-то "не инжектится", первым делом проверять:

- `rootnocombro/internal/di/InitKoin.kt`
- `rootnocombro/internal/di/RootNocombroModule.kt`
- `rootnocombro/internal/di/ModuleFactory.kt`

## Технологии, которые уже точно используются

- Kotlin Multiplatform
- Compose Multiplatform / Compose Desktop
- Decompose
- Koin
- Room
- SQLite bundled
- DataStore
- kotlinx serialization
- KSP
- Detekt
- Dokka

Также в проекте есть собственные convention plugins в `build-logic`.

## Сборка и запуск

Базовые команды, с которых стоит начинать:

```powershell
.\gradlew :app:desktopApp:run
.\gradlew build
.\gradlew detektAll
```

Если нужен конкретный модуль, лучше запускать gradle-задачи адресно через `:<module>:...`.

## Где искать изменения по типовым задачам

### Новый экран или изменение маршрута

Смотреть:

- `rootnocombro/internal/navigation/*`
- `rootnocombro/api/component/*`
- соответствующий `features/...`

### Изменение общей темы или базовых UI-элементов

Смотреть:

- `theme`
- `coreui`

### Настройки приложения

Смотреть:

- `datastore`
- `rootnocombro/api/component/SettingsComponent.kt`

### База данных / сущности / сохранение

Смотреть:

- `database`
- связанные feature-модули, которые используют репозитории/DAO

### Проблемы с запуском приложения

Проверять по порядку:

1. `app/desktopApp/build.gradle.kts`
2. `Main.kt`
3. `initKoin()`
4. root navigation/component wiring

## Практическая заметка для следующих заходов

Чтобы быстро входить в проект без полного сканирования, обычно достаточно открыть:

1. `settings.gradle.kts`
2. `app/desktopApp/src/desktopMain/kotlin/ru/pavlig43/nocombro/Main.kt`
3. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/ui/App.kt`
4. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`
5. нужный `features/.../build.gradle.kts` и его `src`

## Ограничения этого обзора

Это стартовая карта проекта, а не полная архитектурная документация.
Она собрана по структуре модулей, Gradle-конфигурации и ключевым entrypoint/navigation файлам, без глубокого прохода по всем feature-реализациям.
