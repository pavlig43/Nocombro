package ru.pavlig43.manageitem.internal.data

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType

interface ItemEssentialsUi{
    val id: Int
}

internal data class ProductEssentialsUi(
    val displayName: String = "",

    val type: ProductType? = null,

    val createdAt: Long? = null,

    val comment:String ="",

    override val id: Int = 0,
): ItemEssentialsUi

//internal data class DocumentEssentialsUi(
//    val displayName: String = "",
//
//    val type: DocumentType? = null,
//
//    val createdAt: Long? = null,
//
//    val comment:String ="",
//
//    override val id: Int = 0,
//): ItemEssentialsUi

internal data class VendorEssentialsUi(
    val displayName: String = "",

    val comment:String ="",
    override val id: Int = 0,
): ItemEssentialsUi

data class DeclarationEssentialsUi(
    override val id: Int = 0,
    val displayName: String = "",
    val isObserveFromNotification:Boolean = true,
    val createdAt: Long? = null,
    val vendorId: Int? = null,
    val vendorName: String? = null,
    val bestBefore: Long? = null
    ) : ItemEssentialsUi