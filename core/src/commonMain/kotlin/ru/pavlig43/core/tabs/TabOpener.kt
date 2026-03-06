package ru.pavlig43.core.tabs

import kotlinx.datetime.LocalDateTime

interface TabOpener {
    fun openDocumentTab(id: Int)
    fun openProductTab(id: Int)
    fun openVendorTab(id: Int)
    fun openDeclarationTab(id: Int)
    fun openTransactionTab(id: Int)
    fun openBatchMovementTab(batchId: Int, productName: String, start: LocalDateTime, end: LocalDateTime)
}