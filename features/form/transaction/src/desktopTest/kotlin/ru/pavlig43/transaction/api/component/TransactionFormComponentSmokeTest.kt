package ru.pavlig43.transaction.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase
import ru.pavlig43.testkit.scenario
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.internal.create.component.creatableTransactionTypes
import ru.pavlig43.transaction.internal.update.TransactionFormTabsComponent
import ru.pavlig43.transaction.internal.update.TransactionTab
import ru.pavlig43.transaction.internal.update.TransactionTabChild

/**
 * Проверяет динамические вкладки транзакций и безопасное открытие старых типов.
 */
class TransactionFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

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

    /**
     * Создаёт форму для seeded id либо новой транзакции указанного типа.
     *
     * База всегда закрывается после [block], поэтому тесты неподдержанных типов
     * не оставляют временные файлы и соединения.
     */
    suspend fun withTransactionComponent(
        transactionId: Int? = null,
        transactionType: TransactionType? = null,
        block: suspend (TransactionFormComponent) -> Unit,
    ) {
        val managedDatabase = createSeededManagedTestDatabase()
        val resolvedTransactionId = transactionId ?: managedDatabase.database.transactionDao.create(
            Transact(
                transactionType = requireNotNull(transactionType),
                createdAt = LocalDateTime(2026, 7, 13, 12, 0),
                comment = "Unsupported transaction smoke test",
                isCompleted = false,
            )
        ).toInt()
        val dependencies = TransactionFormDependencies(
            db = managedDatabase.database,
            dbTransaction = NocombroTransactionExecutor(managedDatabase.database),
            immutableTableDependencies = ImmutableTableDependencies(managedDatabase.database),
            filesDependencies = FilesDependencies(
                db = managedDatabase.database,
                remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
            ),
        )

        val component = runOnUiThread {
            TransactionFormComponent(
                transactionId = resolvedTransactionId,
                tabOpener = NoopTabOpener,
                componentContext = DefaultComponentContext(
                    lifecycle = LifecycleRegistry(),
                    backHandler = BackDispatcher(),
                ),
                dependencies = dependencies,
            )
        }

        try {
            block(component)
        } finally {
            managedDatabase.close()
        }
    }

    test(
        scenario(
            given = "a seeded sale transaction",
            whenAction = "the transaction form is opened by id",
            thenResult = "sale, reminders and expenses tabs are assembled and sale is selected",
        )
    ) {
        withTransactionComponent(transactionId = 11) { component ->
            val updateChild = waitForUpdateChild(component)
            val builtTabs = waitForTabTypes(
                component = updateChild.component,
                expectedTypes = setOf(
                    TransactionTabChild.Essentials::class.java,
                    TransactionTabChild.Reminders::class.java,
                    TransactionTabChild.Sale::class.java,
                    TransactionTabChild.Expenses::class.java,
                ),
            )

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essentials",
                "Reminders",
                "Sale",
                "Expenses",
            )

            val selected = updateChild.component.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }
            selected.shouldBeInstanceOf<TransactionTabChild.Sale>()
            component.model.value.title shouldBe "*Транзакция Продажа"
        }
    }

    test("write-off and inventory cannot be selected when a transaction is created") {
        creatableTransactionTypes shouldBe listOf(
            TransactionType.BUY,
            TransactionType.SALE,
            TransactionType.OPZS,
        )
    }

    test("existing unsupported transactions open in a non-crashing unsupported mode") {
        listOf(TransactionType.WRITE_OFF, TransactionType.INVENTORY).forEach { transactionType ->
            withTransactionComponent(transactionType = transactionType) { component ->
                val updateChild = waitForUpdateChild(component)
                waitUntil {
                    updateChild.component.unsupportedTransactionType.value == transactionType
                }

                updateChild.component.unsupportedTransactionType.value shouldBe transactionType
                updateChild.component.tabNavigationComponent.tabChildren.value.run {
                    items.map { it.configuration } shouldBe listOf(
                        TransactionTab.Essentials,
                        TransactionTab.Reminders,
                    )
                    selectedIndex shouldBe 0
                }
            }
        }
    }

    test(
        scenario(
            given = "a seeded buy transaction",
            whenAction = "the transaction form is opened by id",
            thenResult = "buy, reminders and expenses tabs are assembled and buy is selected",
        )
    ) {
        withTransactionComponent(transactionId = 1) { component ->
            val updateChild = waitForUpdateChild(component)
            val builtTabs = waitForTabTypes(
                component = updateChild.component,
                expectedTypes = setOf(
                    TransactionTabChild.Essentials::class.java,
                    TransactionTabChild.Reminders::class.java,
                    TransactionTabChild.Buy::class.java,
                    TransactionTabChild.Expenses::class.java,
                ),
            )

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essentials",
                "Reminders",
                "Buy",
                "Expenses",
            )

            val selected = updateChild.component.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }
            selected.shouldBeInstanceOf<TransactionTabChild.Buy>()
            component.model.value.title shouldBe "*Транзакция Покупка"
        }
    }
})
