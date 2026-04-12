---
name: nocombro-di
description: Trace and change Nocombro dependency injection, Koin wiring, module registration, and feature dependency assembly. Use when compilation fails around `scope.get()`, when a feature is added but not wired, when repositories or DAO bindings are missing, or when root scope cannot create a component.
---

# Nocombro DI

Use this skill for Koin and feature wiring tasks. Start from root DI and read feature internals only after the injection path is clear.

## Quick Start

Read these files first:

1. `AGENTS.md`
2. `settings.gradle.kts`
3. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/InitKoin.kt`
4. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/RootNocombroModule.kt`
5. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/ModuleFactory.kt`

Read these only if the task touches persisted dependencies:

- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/DatabaseModule.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/SettingsModule.kt`
- target feature `Dependencies` class

## Workflow

1. Confirm the module is included in `settings.gradle.kts`.
2. Confirm `rootnocombro/build.gradle.kts` depends on the feature.
3. Inspect `InitKoin.kt` for startup module assembly.
4. Inspect `RootNocombroModule.kt` for registrations and exposed factories.
5. Inspect `ModuleFactory.kt` for component creation paths.
6. Inspect the feature dependency class and constructor parameters.
7. Inspect database or datastore modules only if the missing binding comes from persistence.

## Common Cases

### New feature compiles in Gradle but not in runtime wiring

Check:

1. feature dependency class exists
2. root module registers it
3. root scope can resolve every constructor parameter
4. component factory path is reachable from navigation

### `scope.get()` or injection failure

Treat this as wiring first, UI second. Verify the exact binding source before touching screens.

## Guardrails

- Do not assume a Gradle dependency is enough to enable a feature.
- Do not debug Compose UI until the binding graph is confirmed.
- Read only the affected feature's DI surfaces, not the whole module tree.
