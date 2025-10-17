package ru.pavlig43.declarationform.internal.data

internal data class RequiresValuesWithDate(
    val id: Int = 0,
    val name: String = "",
    val isObserveFromNotification:Boolean = true,
    val createdAt: Long? = null,
    val vendorId: Int? = null,
    val vendorName: String? = null,
    val bestBefore: Long? = null
)