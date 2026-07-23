package ru.pavlig43.product.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.product.api.ProductFormDependencies
import ru.pavlig43.product.internal.di.ProductDeclarationRepository
import ru.pavlig43.product.internal.update.ProductFormTabsComponent
import ru.pavlig43.product.internal.update.ProductTabChild
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase
import ru.pavlig43.testkit.scenario

class ProductFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    @Suppress("RedundantSuspendModifier")
    suspend fun waitForUpdateChild(component: ProductFormComponent): ProductFormComponent.Child.Update {
        repeat(50) {
            val child = component.stack.value.active.instance
            if (child is ProductFormComponent.Child.Update) {
                return child
            }
            delay(20)
        }
        error("Product form did not switch to update child in time.")
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun waitForTabTypes(
        component: ProductFormTabsComponent,
        expectedTypes: Set<Class<out ProductTabChild>>,
    ): List<ProductTabChild> {
        repeat(50) {
            val items = component.tabNavigationComponent.tabChildren.value.items.map { it.instance }
            val actualTypes = items.map { it.javaClass }.toSet()
            if (actualTypes.containsAll(expectedTypes)) {
                return items
            }
            delay(20)
        }
        error("Product tabs were not initialized in time.")
    }

    suspend fun withProductComponent(
        productId: Int,
        block: suspend (ProductFormComponent) -> Unit,
    ) {
        val managedDatabase = createSeededManagedTestDatabase()
        val dependencies = ProductFormDependencies(
            db = managedDatabase.database,
            transaction = NocombroTransactionExecutor(managedDatabase.database),
            immutableTableDependencies = ImmutableTableDependencies(managedDatabase.database),
            filesDependencies = FilesDependencies(
                db = managedDatabase.database,
                remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
            ),
        )

        val component = runOnUiThread {
            ProductFormComponent(
                productId = productId,
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
            given = "a seeded FOOD_PF product",
            whenAction = "the product form is opened by id",
            thenResult = "all core product tabs including composition are assembled successfully",
        )
    ) {
        withProductComponent(productId = 3) { component ->
            val updateChild = waitForUpdateChild(component)
            val builtTabs = waitForTabTypes(
                component = updateChild.component,
                expectedTypes = setOf(
                    ProductTabChild.Essentials::class.java,
                    ProductTabChild.Specification::class.java,
                    ProductTabChild.Files::class.java,
                    ProductTabChild.SafetyStock::class.java,
                    ProductTabChild.Declaration1::class.java,
                    ProductTabChild.Composition::class.java,
                ),
            )

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essentials",
                "Specification",
                "Files",
                "SafetyStock",
                "Declaration1",
                "Composition",
            )

            val selected = updateChild.component.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }
            selected.shouldBeInstanceOf<ProductTabChild.Essentials>()
            component.model.value.title shouldBe " Колбаски Баварские"
        }
    }

    test(
        scenario(
            given = "a product linked to one declaration",
            whenAction = "the declaration is replaced",
            thenResult = "the old link becomes a tombstone and the new link is saved",
        )
    ) {
        val managedDatabase = createSeededManagedTestDatabase()
        try {
            val repository = ProductDeclarationRepository(managedDatabase.database)
            val oldLinks = repository.getInit(3).getOrThrow()
            val oldLink = oldLinks.single()
            val replacement = ProductDeclarationIn(
                productId = 3,
                declarationId = 6,
                isProductInDeclaration = true,
            )

            repository.update(ChangeSet(oldLinks, listOf(replacement))).getOrThrow()

            val currentLink = repository.getInit(3).getOrThrow().single()
            currentLink.declarationId shouldBe 6
            currentLink.isProductInDeclaration shouldBe true
            currentLink.syncId shouldNotBe oldLink.syncId

            val tombstone = managedDatabase.database.mirrorDeletionJournalDao.getAll()
                .single { it.entityTable == MirrorSyncTable.PRODUCT_DECLARATION.tableName }
            tombstone.syncId shouldBe oldLink.syncId
        } finally {
            managedDatabase.close()
        }
    }
})
