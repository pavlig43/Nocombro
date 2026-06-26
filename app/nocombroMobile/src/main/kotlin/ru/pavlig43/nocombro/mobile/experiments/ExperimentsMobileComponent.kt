package ru.pavlig43.nocombro.mobile.experiments

import android.content.Context
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase
import ru.pavlig43.nocombro.mobile.experiments.sync.AndroidExperimentSyncTransport
import ru.pavlig43.nocombro.mobile.experiments.sync.MissingAndroidExperimentSyncCredentialsProvider

class ExperimentsMobileComponent(
    componentContext: ComponentContext,
    appContext: Context,
) : ComponentContext by componentContext {
    private val database = MobileExperimentsDatabase.create(appContext)
    private val coroutineScope = componentCoroutineScope()
    private var isClosed = false

    val repository: ExperimentsRepository = RoomExperimentsRepository(
        db = database,
        coroutineScope = coroutineScope,
        syncTransport = AndroidExperimentSyncTransport(
            credentialsProvider = MissingAndroidExperimentSyncCredentialsProvider(),
        ),
    )

    fun close() {
        if (isClosed) return
        isClosed = true
        coroutineScope.cancel()
        database.close()
    }

    private fun componentCoroutineScope(): CoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        if (lifecycle.state != Lifecycle.State.DESTROYED) {
            lifecycle.doOnDestroy {
                close()
            }
        } else {
            scope.cancel()
        }

        return scope
    }
}
