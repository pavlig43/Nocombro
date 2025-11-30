package ru.pavlig43.documentform.internal.component.tabs

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface DocumentTab {
    @Serializable
    data object Create: DocumentTab
    @Serializable
    data object Files: DocumentTab
}