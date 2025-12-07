package ru.pavlig43.rootnocombro.internal.navigation.tab

import kotlinx.serialization.Serializable

@Serializable
sealed interface TabConfig{

    @Serializable
    class Notification:TabConfig
    @Serializable
    sealed interface ItemList: TabConfig{
        @Serializable
        class DocumentList : ItemList

        @Serializable
        class ProductList : ItemList

        @Serializable
        class VendorList:ItemList

        @Serializable
        class DeclarationList:ItemList
    }


    @Serializable
    sealed interface ItemForm: TabConfig{
        @Serializable
        class DocumentForm(val id:Int):ItemForm

        @Serializable
        class ProductForm(val id:Int): ItemForm

        @Serializable
        class VendorForm(val id:Int):ItemForm



        @Serializable
        class DeclarationForm(val id:Int):ItemForm

        @Serializable
        class TransactionForm(val id:Int):ItemForm
    }








}