package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.datastore.DATASTORE_PATH_PROPERTY
import ru.pavlig43.rootnocombro.api.RootDependencies
import ru.pavlig43.rootnocombro.internal.di.initKoin
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil
import ru.pavlig43.testkit.waitUntilNotNull
import ru.pavlig43.testkit.database.createManagedTestDatabase
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase
import ru.pavlig43.testkit.scenario
import java.nio.file.Files
import kotlin.jvm.functions.Function1

class RootNocombroFlowSmokeTest : DesktopMainDispatcherFunSpec({

    suspend fun withRootComponent(
        seeded: Boolean = true,
        block: suspend (RootNocombroComponent) -> Unit,
    ) {
        val managedDatabase = if (seeded) {
            createSeededManagedTestDatabase()
        } else {
            createManagedTestDatabase()
        }
        val dataStorePath = Files.createTempDirectory("nocombro-root-smoke")
            .resolve("preferences.preferences_pb")
        stopKoin()
        System.setProperty(DATASTORE_PATH_PROPERTY, dataStorePath.toString())
        initKoin(databaseOverride = managedDatabase.database)
        val rootDependencies = getKoin().get<RootDependencies>()

        val lifecycle = LifecycleRegistry()
        val component = runOnUiThread {
            lifecycle.resume()
            RootNocombroComponent(
                componentContext = DefaultComponentContext(
                    lifecycle = lifecycle,
                    backHandler = BackDispatcher(),
                ),
                rootDependencies = rootDependencies,
            )
        }

        try {
            block(component)
        } finally {
            stopKoin()
            System.clearProperty(DATASTORE_PATH_PROPERTY)
            managedDatabase.close()
        }
    }

    test(
        scenario(
            given = "a seeded test environment",
            whenAction = "the root component is created",
            thenResult = "tabs mode starts and profitability is the default main tab",
        )
    ) {
        withRootComponent { root ->
            val rootChild = root.stack.value.active.instance
            rootChild.shouldBeInstanceOf<RootChild.Tabs>()

            val tabsComponent = rootChild.component
            val tabChildren = tabsComponent.tabNavigationComponent.tabChildren.value
            tabChildren.items.shouldHaveSize(1)
            tabChildren.selectedIndex shouldBe 0
            tabChildren.items.single().instance.shouldBeInstanceOf<MainTabChild.ProfitabilityChild>()
        }
    }

    test(
        scenario(
            given = "a seeded test environment",
            whenAction = "drawer navigation opens key data screens",
            thenResult = "storage and analytic tabs are created successfully",
        )
    ) {
        withRootComponent { root ->
            val tabsComponent = root.stack.value.active.instance
                .shouldBeInstanceOf<RootChild.Tabs>()
                .component

            runOnUiThread {
                tabsComponent.openScreenFromDrawer(DrawerDestination.Storage)
            }
            val storageSelected = tabsComponent.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }
            storageSelected.shouldBeInstanceOf<MainTabChild.StorageChild>()

            runOnUiThread {
                tabsComponent.openScreenFromDrawer(DrawerDestination.Analytic)
            }
            val analyticSelected = tabsComponent.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }
            analyticSelected.shouldBeInstanceOf<MainTabChild.MainMoneyChild>()
        }
    }

    test(
        scenario(
            given = "a seeded test environment",
            whenAction = "drawer navigation opens the doctor screen",
            thenResult = "the doctor tab is created successfully",
        )
    ) {
        withRootComponent { root ->
            val tabsComponent = root.stack.value.active.instance
                .shouldBeInstanceOf<RootChild.Tabs>()
                .component

            runOnUiThread {
                tabsComponent.openScreenFromDrawer(DrawerDestination.Doctor)
            }

            val selected = tabsComponent.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }
            selected.shouldBeInstanceOf<MainTabChild.DoctorChild>()
        }
    }

    test(
        scenario(
            given = "a seeded test environment",
            whenAction = "form and table tabs are opened for existing entities",
            thenResult = "the root navigation can build core edit and list screens",
        )
    ) {
        withRootComponent { root ->
            val tabsComponent = root.stack.value.active.instance
                .shouldBeInstanceOf<RootChild.Tabs>()
                .component

            runOnUiThread {
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemListConfig.ProductListConfig())
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemFormConfig.ProductFormConfig(id = 1))
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemFormConfig.TransactionFormConfig(id = 11))
            }

            val selected = tabsComponent.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }

            selected.shouldBeInstanceOf<MainTabChild.ItemFormChild.TransactionFormChild>()
            tabsComponent.tabNavigationComponent.tabChildren.value.items.any {
                it.instance is MainTabChild.ImmutableTableChild
            } shouldBe true
        }
    }

    test(
        scenario(
            given = "a seeded test environment",
            whenAction = "core CRUD routes are opened for seeded entities",
            thenResult = "vendor, declaration, expense and related list screens are assembled successfully",
        )
    ) {
        withRootComponent { root ->
            val tabsComponent = root.stack.value.active.instance
                .shouldBeInstanceOf<RootChild.Tabs>()
                .component

            runOnUiThread {
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemListConfig.VendorListConfig())
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemListConfig.DeclarationListConfig())
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemListConfig.ExpenseListConfig())
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemFormConfig.VendorFormConfig(id = 5))
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemFormConfig.DeclarationFormConfig(id = 5))
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemFormConfig.ExpenseFormConfig(id = 1))
            }

            val builtChildren = tabsComponent.tabNavigationComponent.tabChildren.value.items.map { it.instance }

            builtChildren.any { it is MainTabChild.ItemFormChild.VendorFormChild } shouldBe true
            builtChildren.any { it is MainTabChild.ItemFormChild.DeclarationFormChild } shouldBe true
            builtChildren.any { it is MainTabChild.ItemFormChild.ExpenseFormChild } shouldBe true
            builtChildren.count { it is MainTabChild.ImmutableTableChild } shouldBe 3

            builtChildren.last().shouldBeInstanceOf<MainTabChild.ItemFormChild.ExpenseFormChild>()
        }
    }

    test(
        scenario(
            given = "a seeded application session",
            whenAction = "a vendor is created from the form, the tab is closed, and the same vendor is reopened from the vendor list",
            thenResult = "the app flow recreates the vendor tab through the vendor list callback",
        )
    ) {
        withRootComponent(seeded = false) { root ->
            val vendorName = "Test Vendor Root Flow"
            val tabsComponent = root.stack.value.active.instance
                .shouldBeInstanceOf<RootChild.Tabs>()
                .component

            runOnUiThread {
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemFormConfig.VendorFormConfig(id = 0))
            }

            val createComponent = waitUntilNotNull(attempts = 250) {
                val vendorForm = tabsComponent.tabNavigationComponent.tabChildren.value.run {
                    items[selectedIndex!!].instance as? MainTabChild.ItemFormChild.VendorFormChild
                }?.component ?: return@waitUntilNotNull null
                val activeChild = readVendorStackActiveChild(vendorForm)
                val createChild = activeChild.takeIf { it.javaClass.simpleName == "Create" } ?: return@waitUntilNotNull null
                readField(createChild, "component")
            }

            runOnUiThread {
                applyVendorDisplayName(createComponent, vendorName)
            }
            invokeNoArg(createComponent, "create")
            val createdVendorId = getKoin().get<RootDependencies>().database.vendorDao
                .observeOnVendors()
                .first { vendors -> vendors.any { item -> item.displayName == vendorName } }
                .first { item -> item.displayName == vendorName }
                .id
            runOnUiThread {
                invokeIntCallback(createComponent, "onSuccessCreate", createdVendorId)
            }

            waitUntil {
                val selected = tabsComponent.tabNavigationComponent.tabChildren.value.run {
                    items[selectedIndex!!].instance
                }
                val vendorForm = (selected as? MainTabChild.ItemFormChild.VendorFormChild)?.component ?: return@waitUntil false
                vendorForm.model.value.title == "*Поставщик $vendorName" &&
                    readVendorStackActiveChild(vendorForm).javaClass.simpleName == "Update"
            }

            runOnUiThread {
                tabsComponent.tabNavigationComponent.onCloseCurrentTab()
                tabsComponent.tabNavigationComponent.addTab(MainTabConfig.ItemListConfig.VendorListConfig())
            }

            val vendorListChild = waitUntilNotNull {
                tabsComponent.tabNavigationComponent.tabChildren.value.run {
                    items[selectedIndex!!].instance as? MainTabChild.ImmutableTableChild
                }
            }

            val createdVendorRow = createVendorTableRow(
                id = createdVendorId,
                displayName = vendorName,
            )
            runOnUiThread {
                invokeFunction1Field(vendorListChild.component, "onItemClick", createdVendorRow)
            }

            waitUntil {
                val selected = tabsComponent.tabNavigationComponent.tabChildren.value.run {
                    items[selectedIndex!!].instance
                }
                val vendorForm = (selected as? MainTabChild.ItemFormChild.VendorFormChild)?.component ?: return@waitUntil false
                vendorForm.model.value.title == "*Поставщик $vendorName"
            }
        }
    }
})

private fun readVendorStackActiveChild(vendorForm: Any): Any {
    val stackValue = readField(vendorForm, "stack") as com.arkivanov.decompose.value.Value<*>
    val childStack = stackValue.value as com.arkivanov.decompose.router.stack.ChildStack<*, *>
    return childStack.active.instance as Any
}

private fun applyVendorDisplayName(createComponent: Any, displayName: String) {
    val onChangeItem = createComponent.javaClass.methods.first { method ->
        method.name == "onChangeItem" && method.parameterCount == 1
    }
    val updater = object : Function1<Any, Any> {
        override fun invoke(item: Any): Any = copyVendorUi(item, displayName)
    }
    onChangeItem.invoke(createComponent, updater)
}

private fun copyVendorUi(item: Any, displayName: String): Any {
    val copyMethod = item.javaClass.methods.first { method ->
        method.name == "copy" && method.parameterCount == 6
    }
    return copyMethod.invoke(
        item,
        displayName,
        readProperty(item, "comment"),
        readProperty(item, "id"),
        readProperty(item, "syncId"),
        readProperty(item, "updatedAt"),
        readProperty(item, "deletedAt"),
    )
}

private fun createVendorTableRow(id: Int, displayName: String): Any {
    val rowClass = Class.forName(
        "ru.pavlig43.immutable.internal.component.items.vendor.VendorTableUi"
    )
    return rowClass.constructors.single { it.parameterCount == 3 }
        .newInstance(displayName, "", id)
}

private fun invokeFunction1Field(target: Any, fieldName: String, argument: Any) {
    @Suppress("UNCHECKED_CAST")
    val fn = readField(target, fieldName) as Function1<Any, *>
    fn.invoke(argument)
}

private fun invokeIntCallback(target: Any, fieldName: String, argument: Int) {
    @Suppress("UNCHECKED_CAST")
    val fn = readProperty(target, fieldName) as Function1<Int, *>
    fn.invoke(argument)
}

private fun invokeNoArg(target: Any, methodName: String) {
    target.javaClass.methods.first { it.name == methodName && it.parameterCount == 0 }
        .invoke(target)
}


private fun readProperty(target: Any, name: String): Any? {
    val getterName = "get${name.replaceFirstChar { it.uppercase() }}"
    return target.javaClass.methods.firstOrNull { method ->
        method.name == getterName && method.parameterCount == 0
    }?.invoke(target)
        ?: readField(target, name)
}

private fun readField(target: Any, name: String): Any? {
    var current: Class<*>? = target.javaClass
    while (current != null) {
        current.declaredFields.firstOrNull { it.name == name }?.let { field ->
            field.isAccessible = true
            return field.get(target)
        }
        current = current.superclass
    }
    error("Field '$name' not found in ${target.javaClass.name}")
}
