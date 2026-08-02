package ru.pavlig43.rootnocombro.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.datastore.SettingsRepository
import ru.pavlig43.datastore.SyncCheckAttemptStore

class RootDependencies(
    val database: NocombroDatabase,
    val settingsRepository: SettingsRepository,
    val syncCheckAttemptStore: SyncCheckAttemptStore,
)
