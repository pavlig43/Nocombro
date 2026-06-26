---
name: nocombro-dev
description: Broad onboarding and cross-module guidance for the Nocombro Kotlin Multiplatform desktop project. Use when Codex needs high-level project orientation, when a task spans several Nocombro areas at once, or when no narrower Nocombro skill fits. Prefer narrower skills such as navigation, DI, forms, data screens, sync, Doctor, testing, experiments, labels, or theme/settings for local tasks.
---

# Nocombro Dev

Use this skill as a fallback for broad or ambiguous tasks in `Nocombro`. For local work, prefer a narrower skill first.

## Start Narrow

Read these files first unless the task is truly cross-cutting:

1. `AGENTS.md`
2. `settings.gradle.kts`
3. `app/desktopApp/src/desktopMain/kotlin/ru/pavlig43/nocombro/Main.kt`
4. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/RootNocombroComponent.kt`
5. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt`
6. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`

Open `PROJECT_OVERVIEW.md` only when repo-wide context is needed.

## Prefer Narrow Skills

- Use `nocombro-navigation` for routes, tabs, drawer, and screen opening.
- Use `nocombro-di` for Koin wiring and feature dependency assembly.
- Use `nocombro-forms` for document/product/vendor/declaration/transaction/expense forms.
- Use `nocombro-data-screens` for analytics, tables, storage, and files.
- Use `nocombro-sync` for YDB mirror sync, tombstones, S3 recovery, and sync UI.
- Use `nocombro-doctor` for diagnostics, storage overview, file cleanup, and S3 orphan cleanup.
- Use `nocombro-testing` for smoke, real-data, and YDB test routing.
- Use `nocombro-experiments` for experiments, entries, reminders, and cloud reminder email.
- Use `nocombro-labels` for thermal labels, PPTX templates, and related PDF output.
- Use `nocombro-theme-settings` for theme, settings, and app-wide UI surfaces.

## Project Shape

- `app/desktopApp`: desktop launcher, window setup, top-level keyboard and back handling
- `rootnocombro`: root composition, feature assembly, navigation, root DI wiring
- `core`: shared abstractions and cross-feature helpers
- `coreui`: shared Compose/UI utilities
- `theme`: app theme
- `database`: Room and SQLite infrastructure
- `datastore`: persisted settings and preferences
- `features/*`: business features

## Routing Heuristics

- Start from `rootnocombro` for routing, feature assembly, or screen-opening issues.
- Start from the target feature module for local business logic or UI changes.
- Start from `database` or `datastore` for persistence issues.
- Start from `YDB_SYNC.md` before sync or file cleanup code.
- Prefer the repo-local `AGENTS.md` over broad project scanning.

## Guardrails

- Do not scan the whole repo by default.
- Do not change root navigation without checking `MainTabConfig.kt` and `MainTabChild.kt` together.
- Do not assume a Gradle dependency is enough to wire a feature.
