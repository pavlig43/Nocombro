package ru.pavlig43.product.internal.update.tabs.specification

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.files.PRODUCT_SPECIFICATION_FILE_NAME
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.product.internal.update.tabs.composition.CompositionUi
import ua.wwind.table.ColumnSpec

/**
 * Компонент вкладки "Спецификация".
 *
 * Помимо обычного редактирования умеет сгенерировать системный PDF-файл
 * спецификации и попросить родительскую форму обновить вкладку файлов.
 */
internal class ProductSpecificationComponent(
    componentContext: ComponentContext,
    private val productId: Int,
    updateRepository: UpdateSingleLineRepository<ProductSpecification>,
    private val pdfRepository: ProductSpecificationPdfRepository,
    private val compositionGenerator: ProductSpecificationCompositionGenerator,
    private val getCurrentComposition: () -> List<CompositionUi>,
    private val getProductName: () -> String,
    private val onPdfGenerated: suspend () -> Unit,
) : UpdateSingleLineComponent<ProductSpecification, ProductSpecificationUi, ProductSpecificationField>(
    componentContext = componentContext,
    id = productId,
    updateSingleLineRepository = updateRepository,
    componentFactory = specificationComponentFactory,
    mapperToDTO = { toDto() }
) {
    override val title: String = "Спецификация"
    override val columns: ImmutableList<ColumnSpec<ProductSpecificationUi, ProductSpecificationField, Unit>> =
        createProductSpecificationColumns(
            onChangeItem = ::onChangeItem,
            onGenerateComposition = ::generateComposition,
        )

    override val errorMessages: Flow<List<String>> = errorTableMessages

    private val _generationProgress = MutableStateFlow<SpecificationPdfProgressUi?>(null)
    internal val generationProgress = _generationProgress.asStateFlow()

    private val _generationResult = MutableStateFlow<SpecificationPdfResultUi?>(null)
    internal val generationResult = _generationResult.asStateFlow()

    /**
     * Закрывает диалог с результатом генерации.
     */
    internal fun dismissGenerationResult() {
        _generationResult.value = null
    }

    /**
     * Заполняет поле "Состав" из текущей вкладки состава продукта.
     */
    internal fun generateComposition() {
        coroutineScope.launch {
            compositionGenerator.generate(
                productId = productId,
                currentComposition = getCurrentComposition(),
            ).onSuccess { generatedComposition ->
                onChangeItem { current ->
                    current.copy(composition = generatedComposition)
                }
            }.onFailure { throwable ->
                _generationResult.value = SpecificationPdfResultUi(
                    title = "Ошибка генерации состава",
                    message = throwable.message ?: "Не удалось собрать состав из вкладки состава.",
                    isSuccess = false,
                )
            }
        }
    }

    /**
     * Запускает генерацию PDF из текущего UI-состояния вкладки.
     *
     * Тяжёлая работа выполняется в `Dispatchers.IO`, чтобы не блокировать UI и
     * дать кнопке показать состояние загрузки.
     */
    internal fun generatePdf() {
        val productName = getProductName().trim()
        if (productName.isBlank()) {
            _generationResult.value = SpecificationPdfResultUi(
                title = "Нет названия продукта",
                message = "Заполни название на вкладке основных данных перед генерацией спецификации.",
                isSuccess = false,
            )
            return
        }

        val validationErrors = validateAllergensEditorText(item.value.allergens)
        if (validationErrors.isNotEmpty()) {
            _generationResult.value = SpecificationPdfResultUi(
                title = "Некорректные аллергены",
                message = validationErrors.first(),
                isSuccess = false,
            )
            return
        }

        _generationProgress.value = SpecificationPdfProgressUi(
            fileName = PRODUCT_SPECIFICATION_FILE_NAME,
        )
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                pdfRepository.generateAndSave(
                    productName = productName,
                    specification = item.value.toDto(),
                )
            }.onSuccess {
                onPdfGenerated()
                _generationResult.value = SpecificationPdfResultUi(
                    title = "PDF сохранён",
                    message = "Файл спецификации обновлён.",
                    isSuccess = true,
                )
            }.onFailure { throwable ->
                _generationResult.value = SpecificationPdfResultUi(
                    title = "Ошибка генерации",
                    message = throwable.message ?: "Не удалось сгенерировать PDF спецификации.",
                    isSuccess = false,
                )
            }
            _generationProgress.value = null
        }
    }
}

/**
 * UI-состояние активной генерации PDF.
 */
internal data class SpecificationPdfProgressUi(
    val fileName: String,
)

/**
 * Финальный результат генерации PDF, показываемый пользователю.
 */
internal data class SpecificationPdfResultUi(
    val title: String,
    val message: String,
    val isSuccess: Boolean,
)

private val specificationComponentFactory = SingleLineComponentFactory<ProductSpecification, ProductSpecificationUi>(
    initItem = ProductSpecificationUi(
        id = 0,
        productId = 0,
        description = "",
        dosage = "",
        composition = "",
        shelfLifeText = "",
        storageConditions = "",
        appearance = "",
        color = "",
        smell = "",
        taste = "",
        physicalChemicalIndicators = "",
        microbiologicalIndicators = "",
        toxicElements = "",
        allergens = "",
        gmoInfo = "",
        syncId = defaultSyncId(),
        updatedAt = defaultUpdatedAt(),
    ),
    errorFactory = { ui ->
        buildList {
            addAll(validateAllergensEditorText(ui.allergens))
        }
    },
    mapperToUi = { toUi() }
)
