package ru.pavlig43.database.data.common


internal suspend fun isCanSaveWithName(
    id: Int,
    name: String,
    isNameAllowed: suspend  (Int, String) -> Boolean
): Result<Unit> {
    return runCatching {
        require(name.isNotBlank()) { "Имя не может быть пустым" }
        require(isNameAllowed(id, name)) { "Имя уже существует" }
    }
}