package ru.pavlig43.declaration.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
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
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import kotlin.io.path.Path

private const val REAL_DATA_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataDeclarationFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

    test(
        scenario(
            given = "a real copied database dump with declarations",
            whenAction = "the first real declaration form is opened by id",
            thenResult = "essential and files tabs are assembled and the title matches the declaration name",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath!!) { db ->
            val declaration = db.declarationDao.observeOnItems().first().first()
            val dependencies = DeclarationFormDependencies(
                transaction = NocombroTransactionExecutor(db),
                db = db,
                immutableTableDependencies = ImmutableTableDependencies(db),
                filesDependencies = FilesDependencies(
                    db = db,
                    remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
                ),
            )
            val component = runOnUiThread {
                DeclarationFormComponent(
                    declarationId = declaration.id,
                    tabOpener = NoopTabOpener,
                    componentContext = DefaultComponentContext(
                        lifecycle = LifecycleRegistry(),
                        backHandler = BackDispatcher(),
                    ),
                    dependencies = dependencies,
                )
            }

            val updateChild = component.stack.value.active.instance
                .shouldBeInstanceOf<DeclarationFormComponent.Child.Update>()
            val builtTabs = updateChild.component.tabNavigationComponent.tabChildren.value.items.map { it.instance }

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essential",
                "File",
            )
            builtTabs.first().shouldBeInstanceOf<DeclarationTabChild.Essential>()
            builtTabs.last().shouldBeInstanceOf<DeclarationTabChild.File>()
            component.model.value.title shouldBe "*Декларация ${declaration.displayName}"
        }
    }
})
