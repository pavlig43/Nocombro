package ru.pavlig43.core.data

/**
 * Класс используется при записи в бд, чтобы перезаписывать только измененные данные
 */
data class ChangeSet<I : Any>(
    val old: I?,
    val new: I
)