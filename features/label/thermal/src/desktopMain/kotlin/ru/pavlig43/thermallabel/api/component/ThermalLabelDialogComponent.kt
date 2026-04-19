package ru.pavlig43.thermallabel.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.datetime.single.date.DateComponent
import ru.pavlig43.thermallabel.api.data.ThermalLabelTemplateService
import ru.pavlig43.thermallabel.api.model.ThermalLabelDialogUi
import ru.pavlig43.thermallabel.api.model.ThermalLabelGenerationRequest
import ru.pavlig43.thermallabel.api.model.ThermalLabelPrefill
import ru.pavlig43.thermallabel.api.model.ThermalLabelSize

/**
 * Decompose-компонент диалога генерации термоэтикетки.
 *
 * Отвечает за:
 * - загрузку prefill-данных по продукту;
 * - хранение пользовательского выбора размера, даты и массы;
 * - открытие вложенного выбора даты;
 * - запуск генерации PPTX через [ThermalLabelTemplateService].
 */
class ThermalLabelDialogComponent(
    componentContext: ComponentContext,
    private val productId: Int,
    private val productName: String,
    private val defaultDate: LocalDate,
    private val service: ThermalLabelTemplateService,
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()
    private val dialogNavigation = SlotNavigation<ThermalLabelInnerDialog>()
    val onDismissRequest: () -> Unit = onDismissed

    private val _uiState = MutableStateFlow(
        ThermalLabelDialogUi(
            date = defaultDate,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    val dialog: Value<ChildSlot<ThermalLabelInnerDialog, ThermalLabelInnerDialogChild>> = childSlot(
        source = dialogNavigation,
        key = "thermal_label_inner_dialog",
        serializer = ThermalLabelInnerDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild,
    )

    init {
        coroutineScope.launch(Dispatchers.IO) {
            service.loadPrefill(productId = productId, productName = productName)
                .onSuccess { prefill ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                        )
                    }
                    loadedPrefill = prefill
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _message.value = throwable.message ?: "Не удалось загрузить данные этикетки."
                }
        }
    }

    private var loadedPrefill: ThermalLabelPrefill? = null

    fun onSelectSize(
        size: ThermalLabelSize,
    ) {
        _message.value = null
        _uiState.update { it.copy(selectedSize = size) }
    }

    fun openDateDialog() {
        dialogNavigation.activate(ThermalLabelInnerDialog.Date)
    }

    fun onMassChange(
        value: String,
    ) {
        _message.value = null
        _uiState.update { it.copy(massText = value) }
    }

    private fun createDialogChild(
        dialog: ThermalLabelInnerDialog,
        context: ComponentContext,
    ): ThermalLabelInnerDialogChild {
        return when (dialog) {
            ThermalLabelInnerDialog.Date -> ThermalLabelInnerDialogChild.Date(
                DateComponent(
                    componentContext = context,
                    initDate = uiState.value.date,
                    onDismissRequest = dialogNavigation::dismiss,
                    onChangeDate = { newDate ->
                        _uiState.update { it.copy(date = newDate) }
                    },
                )
            )
        }
    }

    fun generate() {
        val current = uiState.value
        val prefill = loadedPrefill

        validate(current.massText).onFailure { throwable ->
            _message.value = throwable.message ?: "Проверь поля этикетки."
            return
        }
        if (prefill == null) {
            _message.value = "Данные этикетки еще загружаются."
            return
        }

        _message.value = null
        _uiState.update { it.copy(isGenerating = true) }
        coroutineScope.launch(Dispatchers.IO) {
            service.generateLabel(
                ThermalLabelGenerationRequest(
                    size = current.selectedSize,
                    productName = prefill.productName,
                    composition = prefill.composition,
                    dosage = prefill.dosage,
                    storageText = prefill.storageText,
                    date = current.date,
                    massText = current.massText,
                )
            ).onSuccess { path ->
                FileKit.openFileWithDefaultApplication(
                    file = PlatformFile(path),
                )
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            }.onFailure { throwable ->
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isGenerating = false) }
                    _message.value = throwable.message ?: "Не удалось сформировать этикетку."
                }
            }
        }
    }
}

/**
 * Минимальная валидация пользовательского ввода перед генерацией этикетки.
 */
private fun validate(
    massText: String,
): Result<Unit> {
    return runCatching {
        require(massText.trim().isNotBlank()) { "Укажи массу." }
    }
}

@Serializable
sealed interface ThermalLabelInnerDialog {
    @Serializable
    data object Date : ThermalLabelInnerDialog
}

/**
 * Вложенные дочерние диалоги, которые может открывать основной диалог этикетки.
 */
sealed interface ThermalLabelInnerDialogChild {
    class Date(val component: DateComponent) : ThermalLabelInnerDialogChild
}
