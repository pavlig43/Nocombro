package ru.pavlig43.rootnocombro.api.component

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

class SyncUiStateTest : DesktopMainDispatcherFunSpec({
    test("startup zeros and old report availability do not mean the databases agree") {
        SyncUiState(remoteSyncConfigured = true, reportAvailable = true, htmlReportAvailable = true)
            .statusSummary() shouldBe "Статус изменений не проверен"
    }

    test("failed status check takes priority over zero counters and a successful old snapshot") {
        SyncUiState(remoteSyncConfigured = true, hasCheckedStatus = true,
            lastError = "ru/pavlig43/database/data/sync/SyncService\$getStatus\$1")
            .statusSummary() shouldBe "Операция синхронизации завершилась с ошибкой"
    }

    test("local winners remote winners and conflicts cannot be described as synchronized") {
        val checked = SyncUiState(remoteSyncConfigured = true, hasCheckedStatus = true)
        checked.copy(pendingLocalChangesCount = 71).statusSummary() shouldBe "Есть изменения для отправки"
        checked.copy(hasRemoteChanges = true, remoteChangesCount = 119).statusSummary() shouldBe "В облаке есть изменения для получения"
        checked.copy(conflictsCount = 1).statusSummary() shouldBe "Есть конфликты: 1"
        checked.statusSummary() shouldBe "Расхождений нет"
    }

    test("missing runtime class error tells the user to restart and preserves the diagnosis") {
        val error = syncOperationErrorMessage(NoClassDefFoundError("ru/pavlig43/database/data/sync/SyncService\$getStatus\$1"))
        error shouldContain "Перезапустите Nocombro"
        error shouldContain "NoClassDefFoundError"
        error shouldContain "SyncService\$getStatus\$1"
        error shouldNotContain "синхронизирована"
    }
})
