package ru.pavlig43.document.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.document.api.DocumentFormDependencies
import ru.pavlig43.document.internal.update.DocumentTabChild
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import kotlin.io.path.Path

private const val REAL_DATA_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataDocumentFormComponentSmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

    test(
        scenario(
            given = "a real copied database dump with documents",
            whenAction = "the first real document form is opened by id",
            thenResult = "essential and files tabs are assembled and the title matches the document name",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = requireNotNull(realDataDatabasePath)) { db ->
            val document = db.documentDao.observeOnDocuments().first().first()
            val dependencies = DocumentFormDependencies(
                transaction = NocombroTransactionExecutor(db),
                db = db,
                filesDependencies = FilesDependencies(
                    db = db,
                    remoteFileStorageGateway = NoopRemoteFileStorageGateway(),
                ),
            )

            val component = runOnUiThread {
                DocumentFormComponent(
                    documentId = document.id,
                    componentContext = DefaultComponentContext(
                        lifecycle = LifecycleRegistry(),
                        backHandler = BackDispatcher(),
                    ),
                    dependencies = dependencies,
                )
            }

            val updateChild = component.stack.value.active.instance
                .shouldBeInstanceOf<DocumentFormComponent.Child.Update>()
            val builtTabs = updateChild.component.tabNavigationComponent.tabChildren.value.items.map { it.instance }

            builtTabs.map { it::class.simpleName }.shouldContainAll(
                "Essential",
                "Files",
            )
            builtTabs.first().shouldBeInstanceOf<DocumentTabChild.Essential>()
            builtTabs.last().shouldBeInstanceOf<DocumentTabChild.Files>()
            component.model.value.title shouldBe "*Документ ${document.displayName}"
        }
    }
})
