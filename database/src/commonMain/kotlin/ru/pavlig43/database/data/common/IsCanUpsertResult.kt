package ru.pavlig43.database.data.common

sealed interface IsCanUpsertResult {
    val message: String

    data class Ok(override val message: String = "OK") : IsCanUpsertResult
    data class NameBlank(override val message: String = "Имя не может быть пустым") :
        IsCanUpsertResult
    data class NameExists(override val message: String = "Имя уже существует") :
        IsCanUpsertResult
}

