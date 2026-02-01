package ru.pavlig43.rootnocombro.internal.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface MainTabConfig{

    @Serializable
    class NotificationConfig:MainTabConfig

    @Serializable
    class SampleTableConfig: MainTabConfig

    @Serializable
    sealed interface ItemListConfig: MainTabConfig{
        @Serializable
        class DocumentListConfig : ItemListConfig

        @Serializable
        class ProductListConfig : ItemListConfig

        @Serializable
        class VendorListConfig:ItemListConfig

        @Serializable
        class DeclarationListConfig:ItemListConfig

        @Serializable
        class TransactionListConfig:ItemListConfig
    }


    @Serializable
    sealed interface ItemFormConfig: MainTabConfig{
        @Serializable
        class DocumentFormConfig(val id:Int):ItemFormConfig

        @Serializable
        class ProductFormConfig(val id:Int): ItemFormConfig

        @Serializable
        class VendorFormConfig(val id:Int):ItemFormConfig

        @Serializable
        class DeclarationFormConfig(val id:Int):ItemFormConfig

        @Serializable
        class TransactionFormConfig(val id:Int):ItemFormConfig
    }

}