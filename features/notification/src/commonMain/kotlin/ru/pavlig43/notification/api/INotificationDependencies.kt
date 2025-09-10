package ru.pavlig43.notification.api

import ru.pavlig43.database.NocombroDatabase

interface INotificationDependencies {
    val db:NocombroDatabase
}