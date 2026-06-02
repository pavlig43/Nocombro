package ru.pavlig43.expense.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.expense.api.ExpenseFormDependencies
import ru.pavlig43.expense.internal.update.ExpenseTabChild
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase
import ru.pavlig43.testkit.scenario

class ExpenseFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    test(
        scenario(
            given = "a seeded expense",
            whenAction = "the expense form is opened by id",
            thenResult = "essentials and files tabs are assembled and the title is populated",
        )
    ) {
        val managedDatabase = createSeededManagedTestDatabase()
        val dependencies = ExpenseFormDependencies(
            transactionExecutor = NocombroTransactionExecutor(managedDatabase.database),
            db = managedDatabase.database,
            filesDependencies = FilesDependencies(
                db = managedDatabase.database,
                remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
            ),
        )

        val component = runOnUiThread {
            ExpenseFormComponent(
                expenseId = 1,
                componentContext = DefaultComponentContext(
                    lifecycle = LifecycleRegistry(),
                    backHandler = BackDispatcher(),
                ),
                dependencies = dependencies,
            )
        }

        try {
            val updateChild = component.stack.value.active.instance
                .shouldBeInstanceOf<ExpenseFormComponent.Child.Update>()
            val builtTabs = updateChild.component.tabNavigationComponent.tabChildren.value.items.map { it.instance }

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essentials",
                "Files",
            )
            builtTabs.first().shouldBeInstanceOf<ExpenseTabChild.Essentials>()
            builtTabs.last().shouldBeInstanceOf<ExpenseTabChild.Files>()
            component.model.value.title shouldBe "*Расход Бензин"
        } finally {
            managedDatabase.close()
        }
    }
})
