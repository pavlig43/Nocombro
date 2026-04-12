package ru.pavlig43.files.api.model

data class LocalOrphanFile(
    val name: String,
    val path: String,
    val relativePath: String,
    val sizeBytes: Long,
)
