package ru.pavlig43.rootnocombro.api.component

data class LocalOrphanFile(
    val name: String,
    val path: String,
    val relativePath: String,
    val sizeBytes: Long,
)
