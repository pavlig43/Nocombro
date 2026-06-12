package ru.pavlig43.experiments.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.files.api.FilesDependencies

class ExperimentsDependencies(
    val database: NocombroDatabase,
    val filesDependencies: FilesDependencies,
)
