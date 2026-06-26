---
name: nocombro-doctor
description: Work on Nocombro Doctor diagnostics, storage overview, local/remote sync diagnostics, file cleanup, S3 orphan cleanup, and safety checks before destructive remote maintenance. Use when changing `features/doctor/*`, `RemoteFilesMaintenanceRepository`, Doctor sync cleanup diagnostics, or UI that reports storage, mirror, or S3 health.
---

# Nocombro Doctor

Use this skill for Doctor tools and diagnostics. Treat Doctor cleanup as safety-sensitive because a wrong S3 decision can delete user files.

## Quick Start

Read these first:

1. `features/doctor/src/desktopMain/kotlin/ru/pavlig43/doctor/api/component/DoctorComponent.kt`
2. `features/doctor/src/desktopMain/kotlin/ru/pavlig43/doctor/api/ui/DoctorScreen.kt`
3. `features/doctor/src/desktopMain/kotlin/ru/pavlig43/doctor/internal/component/DoctorModels.kt`
4. `features/doctor/src/desktopMain/kotlin/ru/pavlig43/doctor/internal/ui/tool/*`
5. `features/files/src/desktopMain/kotlin/ru/pavlig43/files/api/RemoteFilesMaintenanceRepository.kt`
6. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/files/remote/*`

Read `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/SyncComponent.kt` when Doctor output overlaps sync status, mirror counts, or remote errors.

## Workflow

1. Separate diagnostic display, local storage inspection, remote mirror inspection, and destructive cleanup.
2. For S3 cleanup, verify the code refreshes active remote mirror file metadata before offering deletion.
3. Confirm errors block cleanup instead of turning into an empty orphan list.
4. Keep UI labels aligned with mirror semantics; avoid queue/cursor language.
5. Check root navigation and DI only if the Doctor screen or tool cannot be reached.

## Guardrails

- Do not base S3 orphan cleanup only on local Room state.
- Do not allow cleanup when mirror or S3 status is unknown.
- Do not hide remote errors behind a successful-looking empty state.
- Do not remove Doctor tools without checking drawer/routes and root wiring.

## Tests

Use focused checks:

```powershell
.\gradlew :features:files:desktopTest
.\gradlew :rootnocombro:desktopTest --tests "*RootNocombroFlowSmokeTest*"
```

For sync-related Doctor behavior, pair this skill with `nocombro-sync` and run the relevant database mirror tests.
