---
name: nocombro-data-screens
description: Work on Nocombro analytics, tables, storage, files, and other data-heavy screens. Use when changing `features/analytic/*`, `features/table/*`, `features/storage`, `features/files`, or related screen logic, and when deciding whether an issue belongs to feature logic, persistence, or screen opening.
---

# Nocombro Data Screens

Use this skill for data-driven feature screens. Start in the owning feature and only return to root navigation if the screen is unreachable.

## Quick Start

Read these files first:

1. `AGENTS.md`
2. one target feature module:
   - `features/analytic/main`
   - `features/analytic/profitability`
   - `features/table/core`
   - `features/table/immutable`
   - `features/table/mutable`
   - `features/storage`
   - `features/files`
3. `database` or `datastore` only if the bug smells like persistence rather than UI logic

Read navigation files only if the screen is not opening:

- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`

## Workflow

1. Identify the owning feature module.
2. Inspect local screen/component/state files first.
3. Inspect repository or DAO usage only if the data path is wrong.
4. Inspect `database` or `datastore` only if the failure is below feature logic.
5. Return to root navigation only if the screen cannot be reached.

## Common Cases

### Table behavior change

Start in `features/table/*` and look for column definitions, row models, and interaction logic before checking shared UI.

### Analytics or storage issue

Start in the owning feature. Use persistence modules only when data source wiring appears wrong.

### Screen missing from app flow

Escalate to navigation after proving the feature module itself looks correct.

## Guardrails

- Do not read every analytics/table/storage module for one task.
- Do not blame persistence before checking feature state and screen logic.
- Use the Gradle source helper only when a third-party table API is the real blocker.
