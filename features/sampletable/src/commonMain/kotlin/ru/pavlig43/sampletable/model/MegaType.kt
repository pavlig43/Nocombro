package ru.pavlig43.sampletable.model

sealed interface MegaType {
    val displayName: String

    sealed interface Type1 : MegaType {
        data object PodType11 : Type1 {
            override val displayName = "Pod Type 1.1"
        }

        data object PodType12 : Type1 {
            override val displayName = "Pod Type 1.2"
        }
    }

    data object Type2 : MegaType {
        override val displayName = "Type 2"
    }

}