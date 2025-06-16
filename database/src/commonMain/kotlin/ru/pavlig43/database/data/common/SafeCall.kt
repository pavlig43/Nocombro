package ru.pavlig43.database.data.common

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.RequestResult
import ru.pavlig43.database.data.document.Document

private const val ERROR_DATABASE = "Ошибка базы данных "
private suspend fun <T> dataBaseSafeCall(
    daoTag: String,
    operationName: String,
    action: suspend () -> T

): RequestResult<T> {
    return runCatching { action() }.fold(
        onSuccess = { RequestResult.Success(it) },
        onFailure = { throwable ->
            val message = "$daoTag $ERROR_DATABASE $operationName"
            when (throwable) {
                is SQLiteException -> RequestResult.Error(throwable = throwable, message = message)
                else -> RequestResult.Error(throwable = throwable, message = message)
            }
        }
    )
}

suspend fun dbSafeInsert(
    daoTag: String,
    action: suspend () -> Unit
): RequestResult<Unit> {
    return dataBaseSafeCall(daoTag, "insert", action)
}

suspend fun  dbSafeDelete(
    daoTag: String,
    action: suspend () -> Int
): RequestResult<Int> {
    val result = dataBaseSafeCall(daoTag, "delete", action)
    return when (result) {
        is RequestResult.Success -> {
            if (result.data == 0) {
                RequestResult.Error(message = "Не удалось удалить запись")
            } else result
        }
        else -> result
    }
}
suspend fun  dbSafeUpdate(
    daoTag: String,
    action: suspend () -> Int
): RequestResult<Int> {
    val result = dataBaseSafeCall(daoTag, "update", action)
    return when (result) {
        is RequestResult.Success -> {
            if ( result.data == 0) {
                RequestResult.Error(message = "Не удалось обновить запись")
            } else result
        }
        else -> result
    }
}
suspend fun <T> dbSafeGet(
    daoTag: String,
    action: suspend () -> T
): RequestResult<T> {
    return dataBaseSafeCall(daoTag, "get", action)
}

fun <T> dbSafeFlow(
    daoTag: String,
    action: () -> Flow<T>
): Flow<RequestResult<T>> {
    return runCatching { action() }.fold(
        onSuccess = { flow: Flow<T> -> flow.map { value -> RequestResult.Success(value) } },
        onFailure = { throwable ->
            val message = "$daoTag $ERROR_DATABASE GET FLOW"
            flow {
                emit(RequestResult.Error(throwable = throwable, message = message))
            }

        }
    )
}