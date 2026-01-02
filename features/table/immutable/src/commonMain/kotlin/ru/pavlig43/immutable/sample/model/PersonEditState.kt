package ua.wwind.table.sample.model

data class PersonEditState(
    val person: Person? = null,
    val rowIndex: Int? = null,
    val nameError: String = "87897897",
    val ageError: String = "",
    val positionError: String = "",
    val salaryError: String = "",
)
