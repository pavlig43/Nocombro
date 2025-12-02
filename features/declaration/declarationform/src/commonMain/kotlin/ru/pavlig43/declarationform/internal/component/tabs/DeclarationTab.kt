package ru.pavlig43.declarationform.internal.component.tabs

import kotlinx.serialization.Serializable

@Serializable
sealed interface DeclarationTab {
    @Serializable
    data object RequireValues: DeclarationTab
    @Serializable
    data object Files: DeclarationTab
}
