# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Nocombro is a Kotlin Multiplatform application (Android + Desktop) for managing declarations, forms, and documents. It uses Jetpack Compose for UI, Decompose for navigation, Room for database, and Koin for dependency injection.

## Build Commands

### Building and Running
```bash
# Build all modules
./gradlew build

# Android debug build
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Desktop application
./gradlew :composeApp:run

# Clean build
./gradlew clean
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core:test
./gradlew :database:test
```

### Code Quality
```bash
# Run Detekt analysis on all targets
./gradlew detektAll

# Generate documentation
./gradlew dokkaHtml
```

### Creating New Feature Modules
```bash
# Create a new KMP feature module
./gradlew createKmpLib -PmoduleName=your_feature_name
```
This creates a new module in `features/your_feature_name/` with standard KMP structure and adds it to `settings.gradle.kts`.

## Architecture

### Module Structure

**Core Modules:**
- `composeApp/` - Main application entry point, targets Android and Desktop
- `core/` - Shared business logic and domain models
- `coreui/` - Common UI components
- `corekoin/` - Dependency injection utilities (ComponentKoinContext for scoped Koin modules)
- `database/` - Room database with cross-platform support
- `datastore/` - Local settings storage
- `theme/` - Design system and theming
- `rootnocombro/` - Root component that integrates all features

**Feature Modules:**
- `features/form/` - Forms for declarations, documents, products, vendors, transactions
- `features/sign/` - Authentication and document signing
- `features/table/` - Table display (immutable, mutable, core)
- `features/storage/` - File management
- `features/manageitem/` - CRUD operations
- `features/notification/` - Notifications

### Custom Gradle Plugins

The project uses custom convention plugins in `build-logic/convention/`:
- `pavlig43.application` - For the main Android application
- `pavlig43.kmplibrary` - Base KMP library configuration
- `pavlig43.feature` - Feature modules (includes KMP + serialization + Koin + coroutines + Compose + Decompose)
- `pavlig43.decompose` - Decompose navigation setup
- `pavlig43.room` - Room database configuration
- `pavlig43.detekt` - Code analysis configuration

Apply these plugins via `alias(libs.plugins.pavlig43.<plugin>)` in `build.gradle.kts` files.

### Dependency Extensions

Use the custom extensions in `build-logic/convention/src/main/java/ru/pavlig43/convention/extension/DependenciesExtension.kt`:

```kotlin
// In build.gradle.kts files
kotlin {
    commonMainDependencies {
        implementation(projects.core)
        // other dependencies
    }
    androidMainDependencies { /* Android-specific */ }
    desktopDependencies { /* Desktop-specific */ }
}
```

### Component Architecture (Decompose)

Components follow a standard pattern:
- Implement `ComponentContext by componentContext`
- Use `ComponentKoinContext` from `corekoin` for scoped DI modules
- Navigation via `StackNavigation` with `@Serializable` config sealed interfaces
- Components that can be opened as tabs implement `MainTabComponent` interface
- State exposed via `Value<T>` (Decompose) or `StateFlow` (for tab names)

Example structure:
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

### Database (Room)

- Uses Room with `BundledSQLiteDriver` for cross-platform database
- Database defined in `database/src/commonMain/kotlin/ru/pavlig43/database/NocombroDatabase.kt`
- Expect/actual pattern for database constructor: `NocombroDatabaseConstructor`
- Initial data seeded on first launch via `initData()` function
- Entities: FileBD, Document, Vendor, Declaration, Product, Transaction

### Dependency Injection (Koin)

- Each Decompose component creates its own Koin scope via `ComponentKoinContext`
- Scoped modules created per-component (e.g., `createDeclarationFormModule()`)
- Scope is automatically destroyed when component is destroyed (InstanceKeeper)
- Dependencies injected via `scope.get<T>()`

### Version Catalog

All versions managed in `gradle/libs.versions.toml`. Access via:
```kotlin
libs.versions.kotlin
libs.plugins.androidApplication
libraries.compose.runtime
```

## Key Technologies

- **Kotlin 2.3.0** with Compose Compiler plugin
- **Compose Multiplatform 1.10.0** - UI framework
- **Decompose 3.4.0** - Component-based navigation
- **Koin 4.1.1** - Dependency injection
- **Room 2.8.4** - Database (Android + Desktop)
- **Kotlinx Serialization 1.10.0** - JSON serialization
- **Ktor 3.3.3** - Network client
- **Detekt 1.23.8** - Static analysis

## Platform Targets

- **Android**: minSdk 26, targetSdk 35, compileSdk 36
- **Desktop**: JVM target
- JDK 21 required

## Code Quality

- Detekt runs automatically with custom rules for Compose and Decompose
- Run `./gradlew detektAll` before committing
- Use `turbine` for testing Flow/StateFlow in tests

## ✅ Claude Code Integration Test

This PR tests that Claude Code automated review is working correctly after workflow configuration updates.

### Changes:
- Added this test section to verify Claude Code review functionality

### Expected:
- Claude Code should review this PR automatically
- Review should appear as a comment from claude[bot]


