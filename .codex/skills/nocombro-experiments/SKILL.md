---
name: nocombro-experiments
description: Work on the Nocombro experiments lab journal feature, experiment entries, experiment reminders, attached files, database entities/DAOs, feature DI, root navigation, and cloud reminder email integration for experiments. Use when changing `features/experiments/*`, experiment tables in `database`, or reminder email code that reads experiment mirror rows.
---

# Nocombro Experiments

Use this skill for the experiments lab journal feature and its reminder/file integration.

## Quick Start

Read these first:

1. `features/experiments/src/desktopMain/kotlin/ru/pavlig43/experiments/api/component/ExperimentsComponent.kt`
2. `features/experiments/src/desktopMain/kotlin/ru/pavlig43/experiments/api/ui/ExperimentsScreen.kt`
3. `features/experiments/src/desktopMain/kotlin/ru/pavlig43/experiments/internal/data/ExperimentsRepository.kt`
4. `features/experiments/src/desktopMain/kotlin/ru/pavlig43/experiments/internal/di/ExperimentsModule.kt`
5. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/experiment/*`

Read root navigation/DI only when the screen is not reachable:

- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/RootNocombroModule.kt`

## Workflow

1. Decide whether the change is UI state, repository behavior, DB schema/DAO, files, reminders, or navigation.
2. Start inside `features/experiments` for local feature behavior.
3. Use `nocombro-sync` when changing experiment mirror rows, tombstones, or reminder sync fields.
4. Use `nocombro-testing` when adding smoke coverage or real-data checks.
5. Check cloud reminder code only when email delivery or YDB reminder queries are involved.

## Cloud Reminder Touchpoints

For email reminder integration, inspect:

- `cloud/reminder-email/functions/send_daily_emails.py`
- `cloud/reminder-email/ydb/001_email_reminder_schema.sql`
- `cloud/reminder-email/README.md`

Confirm cloud queries use mirror `*_sync_id` fields and filter tombstoned reminders/parents.

## Guardrails

- Do not treat experiments as a generic table-only feature; it has entries, reminders, and files.
- Do not change root navigation before verifying local feature component construction.
- Do not update cloud reminder queries without checking mirror DDL and desktop sync field names.
