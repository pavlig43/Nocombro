package ru.pavlig43.rootnocombro.api

import ru.pavlig43.database.NocombroDatabase

interface IRootDependencies {
    val database:NocombroDatabase
}