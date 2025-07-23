package ru.pavlig43.rootnocombro.internal.navigation.tab.component

import kotlinx.serialization.Serializable

@Serializable
sealed interface TabConfig{
    @Serializable
    class DocumentList : TabConfig

    @Serializable
    class CreateDocument : TabConfig

    @Serializable
    class ChangeDocument(val id:Int) : TabConfig

}