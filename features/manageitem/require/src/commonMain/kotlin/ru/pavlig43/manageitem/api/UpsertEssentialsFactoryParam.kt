package ru.pavlig43.manageitem.api

import ru.pavlig43.itemlist.api.ItemListDependencies

sealed interface UpsertEssentialsFactoryParam{

    val id: Int


    val createItemType: CreateItemType
    val upsertEssentialsDependencies: UpsertEssentialsDependencies

    val onSuccessUpsert: (id: Int) -> Unit
    val onChangeValueForMainTab:(String)-> Unit
}
 enum class CreateItemType{
    Document,
    Product,
    Vendor,
    Declaration
}

data class DocumentFactoryParam(
    override val upsertEssentialsDependencies: UpsertEssentialsDependencies,
    override val onSuccessUpsert: (id: Int) -> Unit,
    override val onChangeValueForMainTab: (String) -> Unit,
    override val id: Int,
): UpsertEssentialsFactoryParam{
    override val createItemType: CreateItemType = CreateItemType.Document
}
data class ProductFactoryParam(
    override val id: Int,
    override val upsertEssentialsDependencies: UpsertEssentialsDependencies,
    override val onSuccessUpsert: (id: Int) -> Unit,
    override val onChangeValueForMainTab: (String) -> Unit
): UpsertEssentialsFactoryParam{
    override val createItemType: CreateItemType = CreateItemType.Product
}
data class VendorFactoryParam(
    override val id: Int,
    override val upsertEssentialsDependencies: UpsertEssentialsDependencies,
    override val onSuccessUpsert: (id: Int) -> Unit,
    override val onChangeValueForMainTab: (String) -> Unit
): UpsertEssentialsFactoryParam{
    override val createItemType: CreateItemType = CreateItemType.Vendor
}
data class DeclarationFactoryParam(
    override val id: Int,
    override val upsertEssentialsDependencies: UpsertEssentialsDependencies,
    override val onSuccessUpsert: (id: Int) -> Unit,
    override val onChangeValueForMainTab: (String) -> Unit,
    val itemListDependencies: ItemListDependencies,
    val onOpenVendorTab: (Int) -> Unit,
): UpsertEssentialsFactoryParam{
    override val createItemType: CreateItemType = CreateItemType.Declaration
}

