---
name: nocombro-theme-settings
description: Change Nocombro theme, settings, app-wide UI surfaces, and desktop-level behavior. Use when editing application theme setup, shared UI foundations, persisted settings, root app composition, or desktop entry behavior that affects more than one screen.
---

# Nocombro Theme Settings

Use this skill for cross-cutting UI and preference work. Start from app-level composition and only drop into feature modules if a setting is consumed there.

## Quick Start

Read these files first:

1. `AGENTS.md`
2. `app/desktopApp/src/desktopMain/kotlin/ru/pavlig43/nocombro/Main.kt`
3. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/ui/App.kt`
4. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/SettingsComponent.kt`
5. `datastore`
6. `theme`
7. `coreui` if the change is shared UI behavior rather than pure theming

## Workflow

1. Decide whether the change is desktop entry, root app composition, theme, or persisted settings.
2. Inspect `Main.kt` for top-level desktop behavior.
3. Inspect `App.kt` for root composition and theme hookup.
4. Inspect `SettingsComponent.kt` and `datastore` for preferences and settings flow.
5. Inspect `theme` and `coreui` only as deeply as the task requires.

## Common Cases

### Theme or visual foundation

Start with `theme`, then `coreui`, then `App.kt` if the theme needs app-level wiring.

### Application settings

Start with `SettingsComponent.kt` and `datastore`, then inspect consumers of that setting.

### App-wide keyboard, window, or desktop behavior

Start with `app/desktopApp/.../Main.kt`.

## Guardrails

- Do not begin from feature modules for app-wide settings unless the bug is clearly local.
- Do not read all of `coreui`; open only the shared control or utility that the task touches.
- Keep `PROJECT_OVERVIEW.md` as a fallback, not a default.
