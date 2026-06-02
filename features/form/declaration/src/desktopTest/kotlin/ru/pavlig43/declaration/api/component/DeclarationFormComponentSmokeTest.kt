package ru.pavlig43.declaration.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.declaration.api.DeclarationFormComponent
import ru.pavlig43.declaration.api.DeclarationFormDependencies
import ru.pavlig43.declaration.internal.update.DeclarationTabChild
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase
import ru.pavlig43.testkit.scenario

class DeclarationFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    test(
        scenario(
            given = "a seeded declaration",
            whenAction = "the declaration form is opened by id",
            thenResult = "essential and files tabs are assembled and the title is populated",
        )
    ) {
        val managedDatabase = createSeededManagedTestDatabase()
        val dependencies = DeclarationFormDependencies(
            transaction = NocombroTransactionExecutor(managedDatabase.database),
            db = managedDatabase.database,
            immutableTableDependencies = ImmutableTableDependencies(managedDatabase.database),
            filesDependencies = FilesDependencies(
                db = managedDatabase.database,
                remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
            ),
        )

        val component = runOnUiThread {
            DeclarationFormComponent(
                declarationId = 5,
                tabOpener = NoopTabOpener,
                componentContext = DefaultComponentContext(
                    lifecycle = LifecycleRegistry(),
                    backHandler = BackDispatcher(),
                ),
                dependencies = dependencies,
            )
        }

        try {
            val updateChild = component.stack.value.active.instance
                .shouldBeInstanceOf<DeclarationFormComponent.Child.Update>()
            val builtTabs = updateChild.component.tabNavigationComponent.tabChildren.value.items.map { it.instance }

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essential",
                "File",
            )
            builtTabs.first().shouldBeInstanceOf<DeclarationTabChild.Essential>()
            builtTabs.last().shouldBeInstanceOf<DeclarationTabChild.File>()
            component.model.value.title shouldBe "*Декларация ИП Гармаш"
        } finally {
            managedDatabase.close()
        }
    }
})
