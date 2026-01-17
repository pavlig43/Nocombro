package ru.pavlig43.document.internal.component.tabs

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface DocumentTab {
    @Serializable
    data object Essentials: DocumentTab
    @Serializable
    data object Files: DocumentTab

}