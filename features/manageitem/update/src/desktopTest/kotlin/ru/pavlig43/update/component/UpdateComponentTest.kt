package ru.pavlig43.update.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil

/** Проверяет разделение ошибок сохранения и постобработки, а также безопасный повтор. */
class UpdateComponentTest : DesktopMainDispatcherFunSpec({
    test("a post-process failure has a separate state and retry does not save again") {
        var saveCalls = 0
        var postProcessCalls = 0
        val component = runOnUiThread {
            UpdateComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                onUpdateAllTabs = {
                    saveCalls++
                    Result.success(Unit)
                },
                errorMessages = flowOf(emptyList()),
                postProcessAfterUpdate = {
                    postProcessCalls++
                    if (postProcessCalls == 1) error("recalculation failed")
                },
            )
        }

        runOnUiThread { component.onUpdate() }
        waitUntil { component.updateState.value is UpdateState.PostProcessError }
        saveCalls shouldBe 1
        postProcessCalls shouldBe 1

        runOnUiThread { component.retryPostProcess() }
        waitUntil { component.updateState.value == UpdateState.Success }
        saveCalls shouldBe 1
        postProcessCalls shouldBe 2
    }

    test("a thrown save failure leaves the Loading state") {
        val component = runOnUiThread {
            UpdateComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                onUpdateAllTabs = { error("save failed") },
                errorMessages = flowOf(emptyList()),
            )
        }

        runOnUiThread { component.onUpdate() }
        waitUntil { component.updateState.value is UpdateState.Error }

        (component.updateState.value as UpdateState.Error).message shouldBe "save failed"
    }
})
