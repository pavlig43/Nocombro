package ru.pavlig43.document.internal.update

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface DocumentTab {

    @Serializable
    data object Essential: DocumentTab

    @Serializable
    data object Files: DocumentTab

}