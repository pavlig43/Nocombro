---
name: nocombro-testing
description: Choose, run, and interpret Nocombro tests, including desktop smoke tests, database tests, form smoke tests, real-data tests, test kit modules, YDB mirror smoke/disaster checks, and pre-commit verification commands. Use when adding tests, diagnosing test failures, deciding a minimal validation set, or working with `test/docs/TEST_RUNBOOK.md`.
---

# Nocombro Testing

Use this skill to pick the smallest useful validation set and interpret failures by subsystem.

## Quick Start

Read `test/docs/TEST_RUNBOOK.md` when the task involves smoke coverage, real-data checks, or failure interpretation.

Useful test infrastructure lives in:

- `test/kit`
- `test/database-kit`
- `database/src/desktopTest`
- `rootnocombro/src/desktopTest`
- target feature `src/desktopTest`

## Core Commands

Run database desktop tests:

```powershell
.\gradlew :database:desktopTest
```

Run smoke groups:

```powershell
.\gradlew smokeDesktop
.\gradlew smokeCoreDesktop
.\gradlew smokeFormsDesktop
```

Run real-data tests with a database dump:

```powershell
.\gradlew :database:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataDatabaseSmokeTest*"
.\gradlew :features:form:product:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataProductFormComponentSmokeTest*"
```

Run YDB mirror checks only when the environment is intentionally configured:

```powershell
$env:NOCOMBRO_YDB_SMOKE = "true"
.\gradlew :database:desktopTest --tests "ru.pavlig43.database.YdbMirrorIntegrationTest" --no-parallel

$env:NOCOMBRO_YDB_DISASTER_RECOVERY = "true"
.\gradlew :database:desktopTest --tests "ru.pavlig43.database.YdbMirrorDisasterRecoveryTest" --no-parallel
```

## Failure Routing

- Database migration or seed failures: start in `database`, schema snapshots, and migration tests.
- Root flow failures: start in root DI/navigation before feature UI.
- Form smoke failures: use `nocombro-forms` and inspect the concrete form module.
- Storage/profitability failures: use `nocombro-data-screens` and inspect feature calculations before persistence.
- Mirror/YDB failures: use `nocombro-sync` and distinguish local planner/apply failures from remote gateway failures.

## Guardrails

- Prefer targeted module tests before a full build.
- Do not require real-data or YDB tests unless the task explicitly needs external data or credentials.
- When a real-data test skips because no dump path is set, treat that as unverified coverage rather than a failure.
- Keep test additions close to the affected module unless shared test-kit behavior is needed.
