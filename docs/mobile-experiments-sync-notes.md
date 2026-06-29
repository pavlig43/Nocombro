# Mobile experiments sync notes

Старая sync-логика из `RoomExperimentsRepository`, чтобы потом вынести её в отдельный репозиторий.

## Snapshot

1. Прочитать все `experiment`.
2. Собрать `experimentSyncIds = experiments.associate { it.id to it.syncId }`.
3. Прочитать все `experiment_entry`.
4. Для каждой записи найти parent `experimentSyncId`; если parent не найден, строку пропустить.
5. Прочитать все `experiment_reminder`.
6. Для каждого напоминания найти parent `experimentSyncId`; если parent не найден, строку пропустить.
7. Собрать `ExperimentSyncSnapshot(experiments, entries, reminders)`.

## Rows

`ExperimentSyncRow`:
- `syncId`
- `title`
- `ideaDescription`
- `isArchived`
- `updatedAt`
- `deletedAt`

`ExperimentEntrySyncRow`:
- `syncId`
- `experimentSyncId`
- `entryDate`
- `content`
- `updatedAt`
- `deletedAt`

`ExperimentReminderSyncRow`:
- `syncId`
- `experimentSyncId`
- `text`
- `reminderDateTime`
- `updatedAt`
- `deletedAt`

## Status

Перед отправкой ставить `SyncStatus.Running`.

После `syncTransport.sync(snapshot)`:
- success: `SyncStatus.Synced(getCurrentLocalDateTime())`
- failure: `SyncStatus.Failed(message ?: "Не удалось синхронизировать")`
