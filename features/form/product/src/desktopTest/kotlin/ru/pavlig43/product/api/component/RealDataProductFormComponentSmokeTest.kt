package ru.pavlig43.product.api.component

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
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.product.api.ProductFormDependencies
import ru.pavlig43.product.internal.update.ProductFormTabsComponent
import ru.pavlig43.product.internal.update.ProductTabChild
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import kotlin.io.path.Path

private const val REAL_DATA_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataProductFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

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

    test(
        scenario(
            given = "a real copied database dump with products",
            whenAction = "the first real product form is opened by id",
            thenResult = "core product tabs are assembled and FOOD_PF also receives composition",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath!!) { db ->
            val product = db.productDao.observeOnProducts().first().first()
            val dependencies = ProductFormDependencies(
                db = db,
                transaction = NocombroTransactionExecutor(db),
                immutableTableDependencies = ImmutableTableDependencies(db),
                filesDependencies = FilesDependencies(
                    db = db,
                    remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
                ),
            )
            val component = runOnUiThread {
                ProductFormComponent(
                    productId = product.id,
                    tabOpener = NoopTabOpener,
                    componentContext = DefaultComponentContext(
                        lifecycle = LifecycleRegistry(),
                        backHandler = BackDispatcher(),
                    ),
                    dependencies = dependencies,
                )
            }

            val updateChild = waitForUpdateChild(component)
            val expectedTypes = buildSet<Class<out ProductTabChild>> {
                add(ProductTabChild.Essentials::class.java)
                add(ProductTabChild.Specification::class.java)
                add(ProductTabChild.Files::class.java)
                add(ProductTabChild.SafetyStock::class.java)
                add(ProductTabChild.Declaration1::class.java)
                if (product.type == ProductType.FOOD_PF) {
                    add(ProductTabChild.Composition::class.java)
                }
            }
            val builtTabs = waitForTabTypes(updateChild.component, expectedTypes)

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essentials",
                "Specification",
                "Files",
                "SafetyStock",
                "Declaration1",
            )
            if (product.type == ProductType.FOOD_PF) {
                builtTabs.map { it::class.simpleName }.shouldContainAll("Composition")
            }

            val selected = updateChild.component.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }
            selected.shouldBeInstanceOf<ProductTabChild.Essentials>()
            component.model.value.title shouldBe " ${product.displayName}"
        }
    }
})
