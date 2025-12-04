package ru.pavlig43.create.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.component.CreateState
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.component.EssentialsComponent
import ru.pavlig43.core.component.toCreateState
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.core.data.ItemEssentialsUi

abstract class CreateEssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    val onSuccessCreate: (Int) -> Unit,
    componentFactory: EssentialComponentFactory<I, T>,
    private val createEssentialsRepository: CreateEssentialsRepository<I>,
    private val mapperToDTO: T.() -> I,
) : EssentialsComponent<I, T>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    getInitData = null,

) {
    private val _createState: MutableStateFlow<CreateState> = MutableStateFlow(CreateState.Init)
    internal val createState = _createState.asStateFlow()

    fun create() {
        coroutineScope.launch(Dispatchers.IO) {
            _createState.update { CreateState.Loading }
            val item = itemFields.value.mapperToDTO()
            val idResult = createEssentialsRepository.createEssential(item)
            _createState.update { idResult.toCreateState() }
        }

    }

}