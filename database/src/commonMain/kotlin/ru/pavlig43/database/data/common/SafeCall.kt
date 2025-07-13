package ru.pavlig43.database.data.common

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.RequestResult

private const val ERROR_DATABASE = "Ошибка базы данных "
suspend fun <T> dbSafeCall(
    daoTag: String,
    action: suspend () -> T

): RequestResult<T> {
    return runCatching { action() }.fold(
        onSuccess = { RequestResult.Success(it) },
        onFailure = { throwable ->
            val message = "$daoTag $ERROR_DATABASE"
            when (throwable) {
                is SQLiteException -> RequestResult.Error(throwable = throwable, message = message)
                else -> RequestResult.Error(throwable = throwable, message = message)
            }
        }
    )
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