package ru.pavlig43.vendor.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase
import ru.pavlig43.testkit.scenario
import ru.pavlig43.vendor.api.VendorFormDependencies
import ru.pavlig43.vendor.internal.update.VendorTabChild

class VendorFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    test(
        scenario(
            given = "a seeded vendor",
            whenAction = "the vendor form is opened by id",
            thenResult = "essential and files tabs are assembled and the title is populated",
        )
    ) {
        val managedDatabase = createSeededManagedTestDatabase()
        val dependencies = VendorFormDependencies(
            transaction = NocombroTransactionExecutor(managedDatabase.database),
            db = managedDatabase.database,
            filesDependencies = FilesDependencies(
                db = managedDatabase.database,
                remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
            ),
        )

        val component = runOnUiThread {
            VendorFormComponent(
                vendorId = 5,
                componentContext = DefaultComponentContext(
                    lifecycle = LifecycleRegistry(),
                    backHandler = BackDispatcher(),
                ),
                dependencies = dependencies,
            )
        }

        try {
            val updateChild = component.stack.value.active.instance
                .shouldBeInstanceOf<VendorFormComponent.Child.Update>()
            val builtTabs = updateChild.component.tabNavigationComponent.tabChildren.value.items.map { it.instance }

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essential",
                "Files",
            )
            builtTabs.first().shouldBeInstanceOf<VendorTabChild.Essential>()
            builtTabs.last().shouldBeInstanceOf<VendorTabChild.Files>()
            component.model.value.title shouldBe "*Поставщик ИП Гармаш"
        } finally {
            managedDatabase.close()
        }
    }
})
