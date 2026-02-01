# CLAUDE_ru.md

Этот файл предоставляет рекомендации Claude Code (claude.ai/code) при работе с кодом в этом репозитории.

## Обзор проекта

Nocombro — это Kotlin Multiplatform приложение (Android + Desktop) для управления декларациями, формами и документами. Использует Jetpack Compose для UI, Decompose для навигации, Room для базы данных и Koin для dependency injection.

## Команды сборки

### Сборка и запуск
```bash
# Сборка всех модулей
./gradlew build

# Сборка Android debug версии
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Desktop приложение
./gradlew :composeApp:run

# Очистка сборки
./gradlew clean
```

### Тестирование
```bash
# Запуск всех тестов
./gradlew test

# Запуск тестов для конкретного модуля
./gradlew :core:test
./gradlew :database:test
```

### Качество кода
```bash
# Запуск Detekt анализа для всех целей
./gradlew detektAll

# Генерация документации
./gradlew dokkaHtml
```

### Создание новых feature-модулей
```bash
# Создание нового KMP feature модуля
./gradlew createKmpLib -PmoduleName=your_feature_name
```
Это создаст новый модуль в `features/your_feature_name/` со стандартной KMP структурой и добавит его в `settings.gradle.kts`.

## Архитектура

### Структура модулей

**Основные модули:**
- `composeApp/` — Точка входа в приложение, цели Android и Desktop
- `core/` — Общая бизнес-логика и доменные модели
- `coreui/` — Общие UI компоненты
- `corekoin/` — Утилиты dependency injection (ComponentKoinContext для scoped Koin модулей)
- `database/` — База данных Room с кроссплатформенной поддержкой
- `datastore/` — Хранение локальных настроек
- `theme/` — Дизайн-система и темизация
- `rootnocombro/` — Корневой компонент, интегрирующий все фичи

**Feature модули:**
- `features/form/` — Формы для деклараций, документов, продуктов, поставщиков, транзакций
- `features/sign/` — Аутентификация и подпись документов
- `features/table/` — Табличное отображение (immutable, mutable, core)
- `features/storage/` — Управление файлами
- `features/manageitem/` — CRUD операции
- `features/notification/` — Уведомления

### Кастомные Gradle плагины

Проект использует кастомные convention плагины в `build-logic/convention/`:
- `pavlig43.application` — Для основного Android приложения
- `pavlig43.kmplibrary` — Базовая конфигурация KMP библиотеки
- `pavlig43.feature` — Feature модули (включает KMP + serialization + Koin + coroutines + Compose + Decompose)
- `pavlig43.decompose` — Настройка навигации Decompose
- `pavlig43.room` — Конфигурация базы данных Room
- `pavlig43.detekt` — Конфигурация анализа кода

Применение этих плагинов через `alias(libs.plugins.pavlig43.<plugin>)` в файлах `build.gradle.kts`.

### Расширения для зависимостей

Используйте кастомные расширения из `build-logic/convention/src/main/java/ru/pavlig43/convention/extension/DependenciesExtension.kt`:

```kotlin
// В build.gradle.kts файлах
kotlin {
    commonMainDependencies {
        implementation(projects.core)
        // другие зависимости
    }
    androidMainDependencies { /* специфичные для Android */ }
    desktopDependencies { /* специфичные для Desktop */ }
}
```

### Архитектура компонентов (Decompose)

Компоненты следуют стандартному паттерну:
- Реализуют `ComponentContext by componentContext`
- Используют `ComponentKoinContext` из `corekoin` для scoped DI модулей
- Навигация через `StackNavigation` с `@Serializable` config sealed interfaces
- Компоненты, которые могут открываться как вкладки, реализуют интерфейс `MainTabComponent`
- Состояние暴露ается через `Value<T>` (Decompose) или `StateFlow` (для имён вкладок)

Пример структуры:
```kotlin
class FeatureComponent(
    componentContext: ComponentContext,
    dependencies: FeatureDependencies,
) : ComponentContext by componentContext, MainTabComponent {
    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope: Scope = koinContext.getOrCreateKoinScope(createFeatureModule(dependencies))

    private val stackNavigation = StackNavigation<Config>()
    val stack: Value<ChildStack<Config, Child>> = childStack(...)

    @Serializable
    sealed interface Config { ... }
    sealed class Child { ... }
}
```

### База данных (Room)

- Использует Room с `BundledSQLiteDriver` для кроссплатформенной базы данных
- База данных определена в `database/src/commonMain/kotlin/ru/pavlig43/database/NocombroDatabase.kt`
- Паттерн expect/actual для конструктора базы данных: `NocombroDatabaseConstructor`
- Начальные данные загружаются при первом запуске через функцию `initData()`
- Сущности: FileBD, Document, Vendor, Declaration, Product, Transaction

### Dependency Injection (Koin)

- Каждый Decompose компонент создаёт свой собственный Koin scope через `ComponentKoinContext`
- Scoped модули создаются для каждого компонента (например, `createDeclarationFormModule()`)
- Scope автоматически уничтожается при уничтожении компонента (InstanceKeeper)
- Зависимости внедряются через `scope.get<T>()`

### Version Catalog

Все версии управляются в `gradle/libs.versions.toml`. Доступ через:
```kotlin
libs.versions.kotlin
libs.plugins.androidApplication
libraries.compose.runtime
```

## Ключевые технологии

- **Kotlin 2.3.0** с плагином Compose Compiler
- **Compose Multiplatform 1.10.0** — UI фреймворк
- **Decompose 3.4.0** — Компонентная навигация
- **Koin 4.1.1** — Dependency injection
- **Room 2.8.4** — База данных (Android + Desktop)
- **Kotlinx Serialization 1.10.0** — JSON сериализация
- **Ktor 3.3.3** — Сетевой клиент
- **Detekt 1.23.8** — Статический анализ

## Целевые платформы

- **Android**: minSdk 26, targetSdk 35, compileSdk 36
- **Desktop**: JVM target
- Требуется JDK 21

## Качество кода

- Detekt запускается автоматически с кастомными правилами для Compose и Decompose
- Запускайте `./gradlew detektAll` перед коммитом
- Используйте `turbine` для тестирования Flow/StateFlow в тестах
