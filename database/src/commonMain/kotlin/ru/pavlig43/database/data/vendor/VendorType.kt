package ru.pavlig43.database.data.vendor

import ru.pavlig43.core.data.ItemType

enum class VendorType(override val displayName: String): ItemType {
    Empty("Пока ничего");

    override val bdName: String = name


}