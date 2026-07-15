package ru.pavlig43.immutable.internal.component.manager

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil

/** Проверяет защиту от двойного удаления и сохранение выделения после ошибки. */
class DeleteManagerTest : DesktopMainDispatcherFunSpec({
    test("loading blocks a duplicate delete and an error preserves the selection") {
        var deleteCalls = 0
        var clearCalls = 0
        var deleteResult: Result<Unit> = Result.failure(IllegalStateException("delete failed"))
        var gate = CompletableDeferred<Unit>()
        val manager = runOnUiThread {
            DeleteManager(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                clearSelection = { clearCalls++ },
                deleteFn = {
                    deleteCalls++
                    gate.await()
                    deleteResult
                },
            )
        }

        runOnUiThread {
            manager.deleteSelected(setOf(1, 2))
            manager.deleteSelected(setOf(1, 2))
        }
        waitUntil { deleteCalls == 1 }
        deleteCalls shouldBe 1
        manager.deleteState.value shouldBe DeleteState.Loading

        gate.complete(Unit)
        waitUntil { manager.deleteState.value is DeleteState.Error }
        clearCalls shouldBe 0

        deleteResult = Result.success(Unit)
        gate = CompletableDeferred<Unit>().also { it.complete(Unit) }
        runOnUiThread { manager.deleteSelected(setOf(1, 2)) }
        waitUntil { manager.deleteState.value == DeleteState.Success }

        deleteCalls shouldBe 2
        clearCalls shouldBe 1
    }
})
