package ru.pavlig43.vendor.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import ru.pavlig43.vendor.api.VendorFormDependencies
import ru.pavlig43.vendor.internal.update.VendorTabChild
import kotlin.io.path.Path

private const val REAL_DATA_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataVendorFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

    test(
        scenario(
            given = "a real copied database dump with vendors",
            whenAction = "the first real vendor form is opened by id",
            thenResult = "essential and files tabs are assembled and the title matches the vendor name",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath!!) { db ->
            val vendor = db.vendorDao.observeOnVendors().first().first()
            val dependencies = VendorFormDependencies(
                transaction = NocombroTransactionExecutor(db),
                db = db,
                filesDependencies = FilesDependencies(
                    db = db,
                    remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
                ),
            )

            val component = runOnUiThread {
                VendorFormComponent(
                    vendorId = vendor.id,
                    componentContext = DefaultComponentContext(
                        lifecycle = LifecycleRegistry(),
                        backHandler = BackDispatcher(),
                    ),
                    dependencies = dependencies,
                )
            }

            val updateChild = component.stack.value.active.instance
                .shouldBeInstanceOf<VendorFormComponent.Child.Update>()
            val builtTabs = updateChild.component.tabNavigationComponent.tabChildren.value.items.map { it.instance }

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essential",
                "Files",
            )
            builtTabs.first().shouldBeInstanceOf<VendorTabChild.Essential>()
            builtTabs.last().shouldBeInstanceOf<VendorTabChild.Files>()
            component.model.value.title shouldBe "*Поставщик ${vendor.displayName}"
        }
    }
})
