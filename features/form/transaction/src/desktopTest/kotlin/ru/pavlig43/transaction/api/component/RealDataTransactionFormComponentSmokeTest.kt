package ru.pavlig43.transaction.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.internal.update.TransactionFormTabsComponent
import ru.pavlig43.transaction.internal.update.TransactionTabChild
import kotlin.io.path.Path

private const val REAL_DATA_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataTransactionFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

    @Suppress("RedundantSuspendModifier")
    suspend fun waitForUpdateChild(component: TransactionFormComponent): TransactionFormComponent.Child.Update {
        repeat(50) {
            val child = component.stack.value.active.instance
            if (child is TransactionFormComponent.Child.Update) {
                return child
            }
            delay(20)
        }
        error("Transaction form did not switch to update child in time.")
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun waitForTabTypes(
        component: TransactionFormTabsComponent,
        expectedTypes: Set<Class<out TransactionTabChild>>,
    ): List<TransactionTabChild> {
        repeat(50) {
            val items = component.tabNavigationComponent.tabChildren.value.items.map { it.instance }
            val actualTypes = items.map { it.javaClass }.toSet()
            if (actualTypes.containsAll(expectedTypes)) {
                return items
            }
            delay(20)
        }
        error("Transaction tabs were not initialized in time.")
    }

    test(
        scenario(
            given = "a real copied database dump with supported transactions",
            whenAction = "the first supported real transaction form is opened by id",
            thenResult = "core tabs are assembled and the selected dynamic tab matches the transaction type",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = requireNotNull(realDataDatabasePath)) { db ->
            val transaction = db.transactionDao.observeOnProductTransactions().first()
                .first { it.transactionType in setOf(TransactionType.BUY, TransactionType.SALE, TransactionType.OPZS) }
            val dependencies = TransactionFormDependencies(
                db = db,
                dbTransaction = NocombroTransactionExecutor(db),
                immutableTableDependencies = ImmutableTableDependencies(db),
                filesDependencies = FilesDependencies(
                    db = db,
                    remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
                ),
            )
            val component = runOnUiThread {
                TransactionFormComponent(
                    transactionId = transaction.id,
                    tabOpener = NoopTabOpener,
                    componentContext = DefaultComponentContext(
                        lifecycle = LifecycleRegistry(),
                        backHandler = BackDispatcher(),
                    ),
                    dependencies = dependencies,
                )
            }

            val updateChild = waitForUpdateChild(component)
            val expectedTypes = when (transaction.transactionType) {
                TransactionType.BUY -> setOf(
                    TransactionTabChild.Essentials::class.java,
                    TransactionTabChild.Reminders::class.java,
                    TransactionTabChild.Files::class.java,
                    TransactionTabChild.Buy::class.java,
                    TransactionTabChild.Expenses::class.java,
                )

                TransactionType.SALE -> setOf(
                    TransactionTabChild.Essentials::class.java,
                    TransactionTabChild.Reminders::class.java,
                    TransactionTabChild.Files::class.java,
                    TransactionTabChild.Sale::class.java,
                    TransactionTabChild.Expenses::class.java,
                )

                TransactionType.OPZS -> setOf(
                    TransactionTabChild.Essentials::class.java,
                    TransactionTabChild.Reminders::class.java,
                    TransactionTabChild.Files::class.java,
                    TransactionTabChild.Pf::class.java,
                    TransactionTabChild.Ingredients::class.java,
                )

                TransactionType.WRITE_OFF,
                TransactionType.INVENTORY -> error("Unsupported transaction type for real-data smoke: ${transaction.transactionType}")
            }
            val builtTabs = waitForTabTypes(updateChild.component, expectedTypes)

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essentials",
                "Reminders",
                "Files",
            )

            when (transaction.transactionType) {
                TransactionType.BUY -> {
                    builtTabs.map { it::class.simpleName }.shouldContainAll("Buy", "Expenses")
                    updateChild.component.tabNavigationComponent.tabChildren.value.run {
                        items[selectedIndex!!].instance.shouldBeInstanceOf<TransactionTabChild.Buy>()
                    }
                }

                TransactionType.SALE -> {
                    builtTabs.map { it::class.simpleName }.shouldContainAll("Sale", "Expenses")
                    updateChild.component.tabNavigationComponent.tabChildren.value.run {
                        items[selectedIndex!!].instance.shouldBeInstanceOf<TransactionTabChild.Sale>()
                    }
                }

                TransactionType.OPZS -> {
                    builtTabs.map { it::class.simpleName }.shouldContainAll("Pf", "Ingredients")
                    updateChild.component.tabNavigationComponent.tabChildren.value.run {
                        items[selectedIndex!!].instance.shouldBeInstanceOf<TransactionTabChild.Pf>()
                    }
                }

                TransactionType.WRITE_OFF,
                TransactionType.INVENTORY -> error("Unsupported transaction type for real-data smoke: ${transaction.transactionType}")
            }

            component.model.value.title shouldBe "*Транзакция ${transaction.transactionType.displayName}"
        }
    }
})
