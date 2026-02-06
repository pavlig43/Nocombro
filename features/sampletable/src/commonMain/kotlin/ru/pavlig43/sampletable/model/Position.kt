package ru.pavlig43.sampletable.model

/** Enum representing various job positions. */
enum class Position(
    val displayName: String,
) {
    JUNIOR_DEVELOPER("Junior Developer"),
    SENIOR_DEVELOPER("Senior Developer"),
    TEAM_LEAD("Team Lead"),
    PROJECT_MANAGER("Project Manager"),
    PRODUCT_MANAGER("Product Manager"),
    DESIGNER("Designer"),
    QA_ENGINEER("QA Engineer"),
    DEVOPS_ENGINEER("DevOps Engineer"),
    DATA_ANALYST("Data Analyst"),
    HR_SPECIALIST("HR Specialist"),
    ;

    override fun toString(): String = displayName
}
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

    companion object {
        val entries: List<MegaType> = listOf(
            Type1.PodType11,
            Type1.PodType12,
            Type2,
        )
    }
}
