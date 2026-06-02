package ru.pavlig43.expense.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.expense.api.ExpenseFormDependencies
import ru.pavlig43.expense.internal.update.ExpenseTabChild
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import kotlin.io.path.Path

private const val REAL_DATA_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataExpenseFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

    test(
        scenario(
            given = "a real copied database dump with expenses",
            whenAction = "the first real expense form is opened by id",
            thenResult = "essentials and files tabs are assembled and the title matches the expense type",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath!!) { db ->
            val expense = db.expenseDao.observeAll().first().first()
            val dependencies = ExpenseFormDependencies(
                transactionExecutor = NocombroTransactionExecutor(db),
                db = db,
                filesDependencies = FilesDependencies(
                    db = db,
                    remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
                ),
            )

            val component = runOnUiThread {
                ExpenseFormComponent(
                    expenseId = expense.id,
                    componentContext = DefaultComponentContext(
                        lifecycle = LifecycleRegistry(),
                        backHandler = BackDispatcher(),
                    ),
                    dependencies = dependencies,
                )
            }

            val updateChild = component.stack.value.active.instance
                .shouldBeInstanceOf<ExpenseFormComponent.Child.Update>()
            val builtTabs = updateChild.component.tabNavigationComponent.tabChildren.value.items.map { it.instance }

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essentials",
                "Files",
            )
            builtTabs.first().shouldBeInstanceOf<ExpenseTabChild.Essentials>()
            builtTabs.last().shouldBeInstanceOf<ExpenseTabChild.Files>()
            component.model.value.title shouldBe "*Расход ${expense.expenseType.displayName}"
        }
    }
})
