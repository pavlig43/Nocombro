package ru.pavlig43.notification.api.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface NotificationLevel {
    val name:String

    @Serializable
    data object Zero : NotificationLevel {
        override val name: String = "zero"
    }

    @Serializable
    data object One : NotificationLevel{
        override val name: String = "one"
    }

    @Serializable
    data object Two : NotificationLevel{
        override val name: String = "two"
    }
}