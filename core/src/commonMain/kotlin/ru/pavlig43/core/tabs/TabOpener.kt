package ru.pavlig43.core.tabs

interface TabOpener {
    fun openDocumentTab(id: Int)
    fun openProductTab(id: Int)
    fun openVendorTab(id: Int)
    fun openDeclarationTab(id: Int)
    fun openTransactionTab(id: Int)
}