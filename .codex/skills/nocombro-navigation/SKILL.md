---
name: nocombro-navigation
description: Trace and change Nocombro root navigation, tab routing, drawer wiring, and screen opening paths. Use when a screen exists but does not open, when adding a new tab or route, when changing how a feature is reachable, or when debugging route-to-component mapping in `rootnocombro`.
---

# Nocombro Navigation

Use this skill for routing tasks in `Nocombro`. Read narrowly and prefer `rootnocombro` before feature internals.

## Quick Start

Read these files first:

1. `AGENTS.md`
2. `settings.gradle.kts`
3. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/RootNocombroComponent.kt`
4. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt`
5. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`
6. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabNavigationComponent.kt`

Read drawer or tab UI only if the route is present but not visible in navigation chrome:

- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/drawer/ui/NavigationDrawer.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/tab/ui/Tab.kt`

## Workflow

1. Confirm whether the task is top-level flow or main-tab flow.
2. Inspect `RootNocombroComponent.kt` for `Sign` vs `Tabs` decisions.
3. Inspect `MainTabConfig.kt` for route arguments and route identity.
4. Inspect `MainTabChild.kt` for route-to-child mapping.
5. Inspect `MainTabNavigationComponent.kt` for child creation and navigation actions.
6. Read the target feature module only after the route path is confirmed.

## Common Cases

### Add a new screen or tab

Usually touch all of these:

1. Target `features/...` module
2. `rootnocombro/build.gradle.kts`
3. `rootnocombro/internal/navigation/MainTabConfig.kt`
4. `rootnocombro/internal/navigation/MainTabChild.kt`
5. `rootnocombro/internal/navigation/MainTabNavigationComponent.kt`
6. Drawer or tab UI if the screen must be user-reachable

### Screen exists but does not open

Check in this order:

1. `MainTabConfig.kt`
2. `MainTabChild.kt`
3. `MainTabNavigationComponent.kt`
4. `RootNocombroComponent.kt` if the issue is above tabs
5. Feature component construction only after routing is verified

## Guardrails

- Do not edit root navigation in isolation; inspect `MainTabConfig.kt` and `MainTabChild.kt` together.
- Do not assume a feature bug before proving the route is created and mapped.
- Skip `PROJECT_OVERVIEW.md` unless the task expands beyond navigation.
