package ru.pavlig43.doctor.internal.component

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.database.data.sync.mirror.VendorMirrorRow

/** Проверяет сортировку полей, версии и статусы строк в карточке конфликта. */
class DoctorSyncConflictViewTest : FunSpec({
    test("conflict view contains sorted serialized fields and separate row statuses") {
        val localVersion = LocalDateTime.parse("2026-07-14T10:00:00")
        val remoteVersion = LocalDateTime.parse("2026-07-14T11:00:00")
        val local = VendorMirrorRow(
            syncId = "vendor-sync-id",
            displayName = "Local name",
            comment = "Local comment",
            updatedAt = localVersion,
        )
        val remote = local.copy(
            displayName = "Remote name",
            comment = "Remote comment",
            updatedAt = LocalDateTime.parse("2026-07-14T09:00:00"),
            deletedAt = remoteVersion,
        )

        val view = MirrorVersionConflict(MirrorSyncTable.VENDOR, local, remote).toDoctorView()

        view.table shouldBe "vendor"
        view.syncId shouldBe "vendor-sync-id"
        view.localVersion shouldBe localVersion
        view.remoteVersion shouldBe remoteVersion
        view.localStatus shouldBe DoctorMirrorRowStatus.ACTIVE
        view.remoteStatus shouldBe DoctorMirrorRowStatus.DELETED
        view.differences.map { it.field } shouldContainExactly listOf(
            "comment",
            "deletedAt",
            "displayName",
            "updatedAt",
        )
        view.differences.first { it.field == "displayName" }.localValue shouldBe "Local name"
        view.differences.first { it.field == "displayName" }.remoteValue shouldBe "Remote name"
    }
})
