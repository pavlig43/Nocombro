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
import ua.wwind.table.ColumnSpec

internal class ProductSpecificationComponent(
    componentContext: ComponentContext,
    productId: Int,
    updateRepository: UpdateSingleLineRepository<ProductSpecification>,
    private val pdfRepository: ProductSpecificationPdfRepository,
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
            onChangeItem = ::onChangeItem
        )

    override val errorMessages: Flow<List<String>> = errorTableMessages

    private val _generationProgress = MutableStateFlow<SpecificationPdfProgressUi?>(null)
    internal val generationProgress = _generationProgress.asStateFlow()

    private val _generationResult = MutableStateFlow<SpecificationPdfResultUi?>(null)
    internal val generationResult = _generationResult.asStateFlow()

    internal fun dismissGenerationResult() {
        _generationResult.value = null
    }

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

internal data class SpecificationPdfProgressUi(
    val fileName: String,
)

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
    errorFactory = { emptyList() },
    mapperToUi = { toUi() }
)
