package ru.pavlig43.core.data

data class ChangeSet<I : Any>(
    val old: I?,
    val new: I
)