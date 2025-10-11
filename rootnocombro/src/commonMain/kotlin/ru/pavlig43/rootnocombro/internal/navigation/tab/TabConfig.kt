package ru.pavlig43.rootnocombro.internal.navigation.tab

import kotlinx.serialization.Serializable

@Serializable
sealed interface TabConfig{

    @Serializable
    class Notification:TabConfig

    @Serializable
    class DocumentList : TabConfig

    @Serializable
    class DocumentForm(val id:Int):TabConfig

    @Serializable
    class ProductList : TabConfig

    @Serializable
    class ProductForm(val id:Int): TabConfig


    @Serializable
    class VendorList:TabConfig

    @Serializable
    class VendorForm(val id:Int):TabConfig







}