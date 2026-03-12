package ru.pavlig43.rootnocombro.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.datastore.SettingsRepository

class RootDependencies (
    val database:NocombroDatabase,
    val settingsRepository:SettingsRepository
    )