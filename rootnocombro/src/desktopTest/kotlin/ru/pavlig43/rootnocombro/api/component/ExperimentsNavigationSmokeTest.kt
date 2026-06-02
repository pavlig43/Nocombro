package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.types.shouldBeInstanceOf
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.datastore.DATASTORE_PATH_PROPERTY
import ru.pavlig43.rootnocombro.api.RootDependencies
import ru.pavlig43.rootnocombro.internal.di.initKoin
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.scenario
import java.nio.file.Files

class ExperimentsNavigationSmokeTest : DesktopMainDispatcherFunSpec({

    suspend fun withRootComponent(block: suspend (RootNocombroComponent) -> Unit) {
        val managedDatabase = createSeededManagedTestDatabase()
        val dataStorePath = Files.createTempDirectory("nocombro-experiments-nav")
            .resolve("preferences.preferences_pb")
        stopKoin()
        System.setProperty(DATASTORE_PATH_PROPERTY, dataStorePath.toString())
        initKoin {
            modules(
                module {
                    single<NocombroDatabase> { managedDatabase.database }
                }
            )
        }
        val rootDependencies = getKoin().get<RootDependencies>()
        val component = runOnUiThread {
            RootNocombroComponent(
                componentContext = DefaultComponentContext(
                    lifecycle = LifecycleRegistry(),
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
            whenAction = "drawer navigation opens experiments",
            thenResult = "the experiments tab is created successfully",
        )
    ) {
        withRootComponent { root ->
            val tabsComponent = root.stack.value.active.instance
                .shouldBeInstanceOf<RootChild.Tabs>()
                .component

            runOnUiThread {
                tabsComponent.openScreenFromDrawer(DrawerDestination.Experiments)
            }

            val selected = tabsComponent.tabNavigationComponent.tabChildren.value.run {
                items[selectedIndex!!].instance
            }
            selected.shouldBeInstanceOf<MainTabChild.ExperimentsChild>()
        }
    }
})
