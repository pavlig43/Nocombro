package ru.pavlig43.testkit

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.tabs.TabOpener
import javax.swing.SwingUtilities

fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()

    var result: T? = null
    var error: Throwable? = null

    SwingUtilities.invokeAndWait {
        try {
            result = block()
        } catch (t: Throwable) {
            error = t
        }
    }

    error?.let { throw it }
    @Suppress("UNCHECKED_CAST")
    return result as T
}

object NoopTabOpener : TabOpener {
    override fun openDocumentTab(id: Int) = Unit
    override fun openProductTab(id: Int) = Unit
    override fun openVendorTab(id: Int) = Unit
    override fun openDeclarationTab(id: Int) = Unit
    override fun openTransactionTab(id: Int) = Unit
    override fun openExperimentTab(id: Int) = Unit
    override fun openBatchMovementTab(
        batchId: Int,
        productName: String,
        start: LocalDateTime,
        end: LocalDateTime,
    ) = Unit

    override fun openExpenseFormTab(id: Int) = Unit
    override fun openProfitabilityTab() = Unit
}
