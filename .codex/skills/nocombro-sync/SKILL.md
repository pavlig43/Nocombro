---
name: nocombro-sync
description: Work on Nocombro YDB mirror synchronization, Room sync metadata, mirror tables, tombstones, S3 file recovery, sync status UI, or sync/file cleanup safety. Use when changing `database/data/sync/mirror/*`, `YDB_SYNC.md`, `database/ydb/*`, `SyncComponent`, remote file repair/download, or any code that relies on `sync_id`, `updated_at`, `deleted_at`, or deletion tombstones.
---

# Nocombro Sync

Use this skill for YDB mirror sync and file recovery work. Start with the sync docs and invariants before reading implementation details.

## Quick Start

Read these first:

1. `YDB_SYNC.md`
2. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorSyncTable.kt`
3. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorReconciliationService.kt`
4. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/sync/mirror/MirrorDeletionJournalRepository.kt`
5. `database/ydb/mirror_sync_v1.sql`
6. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/SyncComponent.kt`

Read `features/files`, `features/doctor`, and `database/data/files/remote/*` only when the task involves file recovery, S3 cleanup, or Doctor diagnostics.

## Invariants

- Legacy queue transport is gone; do not bring back `sync_change`, cursor paging, JSON payload sync, or legacy gateway assumptions.
- `sync_id`, `updated_at`, `deleted_at`, and tombstones are core sync invariants.
- Relationships between mirrored entities use `*_sync_id`, not local Room `id`.
- Hard deletes must be captured through the deletion journal so remote tombstones can be produced.
- S3 cleanup must compare against active remote mirror `file` metadata, not only the current local Room database.
- `database/ydb/drop_legacy_sync_tables.sql` is manual operator SQL; the app must not run it.

## Workflow

1. Identify whether the change is mirror schema, local snapshot/apply, remote gateway, Sync UI, files/S3, or Doctor.
2. Check `YDB_SYNC.md` for intended behavior before editing code.
3. Trace the data flow as snapshot -> reconciliation plan -> local apply/remote upsert -> status or UI.
4. For deletes, inspect journal capture and tombstone application before changing DAO or repository code.
5. For files, verify metadata sync and binary S3 behavior separately.
6. Use Doctor as a diagnostic surface after core sync behavior is understood.

## Tests

Use narrow tests first:

```powershell
.\gradlew :database:desktopTest --tests "*MirrorReconciliationPlannerTest*"
.\gradlew :database:desktopTest --tests "*MirrorLocalApplyRepositoryTest*"
.\gradlew :database:desktopTest --tests "*SyncServiceMirrorStatusTest*"
```

For file repair or cleanup:

```powershell
.\gradlew :database:desktopTest --tests "*RemoteFileBatchDownloadRepositoryTest*"
.\gradlew :features:files:desktopTest
```

For real YDB checks, only run when credentials and environment are intended for it:

```powershell
$env:NOCOMBRO_YDB_SMOKE = "true"
.\gradlew :database:desktopTest --tests "ru.pavlig43.database.YdbMirrorIntegrationTest" --no-parallel

$env:NOCOMBRO_YDB_DISASTER_RECOVERY = "true"
.\gradlew :database:desktopTest --tests "ru.pavlig43.database.YdbMirrorDisasterRecoveryTest" --no-parallel
```

For manual two-device verification, inspect `tools/run-device2.ps1` and keep separate app-data roots.
