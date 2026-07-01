# Mobile module refactor report

## DI

- `NocombroMobileModule.kt`: оставлен только app-level DI: mobile database, `ExperimentDependencies`, `NocombroMobileRootDependencies`.
- `experiments/internal/di/MobileExperimentsModule.kt`: repositories перенесены в feature-level DI factory.
- `ExperimentsListRepository` больше не создаётся дважды в одном DI-файле.

## Database

- `MobileExperimentsDatabase` переименована в `NocombroMobileDatabase`, потому что это БД всего mobile app.
- DAO перенесены в `internal/database/dao`.
- Room entities перенесены в `internal/database/entity`.
- Schema folder перенесён под новый database class name.

## Navigation And Dependencies

- Root navigation types (`MobileConfig`, `MobileChild`) перенесены в `NocombroMobileRootComponent.kt`, рядом с местом использования.
- `NocombroMobileRootDependencies` вынесен в отдельный файл.
- `ExperimentDependencies` вынесен в отдельный файл по desktop-паттерну.

## Experiment Details Component

- Backing state переименован в `_experimentDraft`, `_message`, `_reminderEditor`.
- Удалён неиспользуемый `experimentDraftState`.
- `experiment` переименован в `selectedExperiment`; null оставлен только как состояние загрузки/удалённой строки.
- `uiState` разбит на `contentState` и финальный combine с message/editor.
- `DetailsExtraState` удалён.
- `init` удалён: синхронизация draft и автосохранение теперь идут через flow chain.
- `collectLatest` в автосохранении заменён на `onEach + launchIn`.
- `openTodayEntry` переименован в `createEntryForToday`.
- `toggleArchive` заменён на `setArchived(isArchived)`, UI передаёт готовый флаг.
- `openCreateReminder`, `openEditReminder`, `dismissReminderEditor` переименованы в `showNewReminderEditor`, `showReminderEditor`, `closeReminderEditor`.
- Пустой `map { }` заменён на явное сведение `Result` к `Result<Unit>`.
- Добавлен KDoc к неочевидному state и автосохранению.

## Unused Code

- Из `ExperimentsListRepository` удалены неиспользуемые `observeExperiment` и `setExperimentArchived`.
