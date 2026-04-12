package ru.pavlig43.files.api.model

data class RemoteOrphanFile(
    val objectKey: String,
    val sizeBytes: Long? = null,
)
