package ru.pavlig43.product.internal.update.tabs.declaration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.DeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.internal.component.items.declaration.DeclarationTableUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.product.internal.di.ProductDeclarationRepository
import ru.pavlig43.tablecore.manger.SelectionManager
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

/**
 * Вкладка деклараций внутри формы продукта.
 *
 * Компонент решает две задачи:
 * 1. управляет связями "продукт -> декларация";
 * 2. умеет проверить, встречается ли имя продукта в одном из файлов
 *    выбранной декларации.
 *
 * Для парсинга компонент берет текущее имя продукта через [getProductName].
 * Это имя протаскивается из вкладки "Основное" родительским
 * [ru.pavlig43.product.internal.update.ProductFormTabsComponent], чтобы здесь
 * всегда использовалось последнее введенное пользователем значение.
 */
internal class ProductDeclarationComponent(
    componentContext: ComponentContext,
    productId: Int,
    private val repository: ProductDeclarationRepository,
    private val tabOpener: TabOpener,
    immutableTableDependencies: ImmutableTableDependencies,
    private val getProductName: () -> String,
) : ComponentContext by componentContext, FormTabComponent {

    private val coroutineScope = componentCoroutineScope()
    private val parser = DeclarationProductPdfParser()
    override val title: String = "Декларации"
    override suspend fun onUpdate(): Result<Unit> {
        val old = initDataComponent.firstData.value
        val new = productDeclarations.value
        return repository.update(ChangeSet(old, new))
    }

    private val productDeclarations = MutableStateFlow<List<ProductDeclarationIn>>(emptyList())

    val initDataComponent = LoadInitDataComponent<List<ProductDeclarationIn>>(
        componentContext = childContext("init"),
        getInitData = {
            repository.getInit(productId)
        },
        onSuccessGetInitData = { lst ->
            productDeclarations.update { lst }
        }
    )

    override suspend fun refreshDataAfterUpsert() {
        initDataComponent.retryLoadInitData()
    }
    private val dialogNavigation = SlotNavigation<DeclarationDialogConfig>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "declaration_dialog",
        serializer = DeclarationDialogConfig.serializer(),
        handleBackButton = true,
    ) { _, context ->
        MBSImmutableTableComponent<DeclarationTableUi>(
            componentContext = context,
            onDismissed = dialogNavigation::dismiss,
            dependencies = immutableTableDependencies,
            immutableTableBuilderData = DeclarationImmutableTableBuilder(
                withCheckbox = false
            ),
            tabOpener = tabOpener,
            onItemClick = { dec: DeclarationTableUi ->
                val declarationIn = ProductDeclarationIn(
                    productId = productId,
                    declarationId = dec.composeId,
                    id = 0
                )
                addDeclaration(declarationIn)
                dialogNavigation.dismiss()
            },
        )
    }

    private fun showDialog() {
        dialogNavigation.activate(DeclarationDialogConfig)
    }

    private val parseSheetUi = MutableStateFlow<ParseDeclarationSheetUi?>(null)
    internal val currentParseSheetUi: StateFlow<ParseDeclarationSheetUi?> = parseSheetUi

    private val parseProgressUi = MutableStateFlow<ParseProgressUi?>(null)
    internal val currentParseProgressUi: StateFlow<ParseProgressUi?> = parseProgressUi

    private val parseResultUi = MutableStateFlow<ParseResultUi?>(null)
    internal val currentParseResultUi: StateFlow<ParseResultUi?> = parseResultUi

    private val selectionManager =
        SelectionManager(
            childContext("selection")
        )

    /**
     * Таблица объединяет:
     * - локальные связи продукта с декларациями;
     * - актуальные данные самих деклараций из БД;
     * - выбранные строки;
     * - id декларации, которая сейчас парсится.
     *
     * Отдельно `parseProgressUi` сюда добавлен затем, чтобы в строке нужной
     * декларации можно было показать состояние загрузки (`isParsing`) и
     * заблокировать кнопку "Парсить" только для нее.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val tableData: StateFlow<TableData<ProductDeclarationTableUi>> = combine(
        productDeclarations,
        selectionManager.selectedIdsFlow,
        parseProgressUi,
    ) { declarations, selectedIds, parseProgress ->
        Triple(declarations.map { it.declarationId }, selectedIds, parseProgress?.declarationId)
    }.flatMapLatest { (declarationIds, selectedIds, parsingDeclarationId) ->
        repository.observeOnDeclarations(declarationIds)
            .map { declarationsList ->
                val declarationsById = productDeclarations.value.associateBy(ProductDeclarationIn::declarationId)
                val tableUiItems = declarationsList.map { declaration ->
                    val declarationIn = declarationsById.getValue(declaration.id)
                    ProductDeclarationTableUi(
                        declarationId = declaration.id,
                        declarationName = declaration.displayName,
                        vendorName = declaration.vendorName,
                        isProductInDeclaration = declarationIn.isProductInDeclaration,
                        isParsing = declaration.id == parsingDeclarationId,
                        isActual = declaration.isActual
                    )
                }
                TableData(
                    displayedItems = tableUiItems,
                    selectedIds = selectedIds,
                    isSelectionMode = true
                )
            }
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData(isSelectionMode = true)
    )


    val columns: ImmutableList<ColumnSpec<ProductDeclarationTableUi, ProductDeclarationField, TableData<ProductDeclarationTableUi>>> =
        createProductDeclarationColumn(::onEvent)

    /**
     * Добавляет декларацию в локальное состояние вкладки, если такой связи еще
     * нет. Сохранение в БД происходит позднее в [onUpdate].
     */
    fun addDeclaration(declaration: ProductDeclarationIn) {
        if (declaration.declarationId in productDeclarations.value.map { it.declarationId }) return
        productDeclarations.update { it + declaration }
    }

    internal fun dismissParseSheet() {
        parseSheetUi.update { null }
    }

    internal fun setPdfFilter(onlyPdf: Boolean) {
        parseSheetUi.update { currentUi ->
            currentUi?.copy(onlyPdf = onlyPdf)
        }
    }

    internal fun dismissParseResult() {
        parseResultUi.update { null }
    }

    /**
     * Обрабатывает выбор файла из bottom sheet.
     *
     * Здесь выполняются быстрые проверки до запуска тяжелого парсинга:
     * поддерживается ли расширение и заполнено ли имя продукта.
     */
    internal fun parseSelectedFile(filePath: String) {
        val sheetUi = parseSheetUi.value ?: return
        val fileUi = sheetUi.files.firstOrNull { it.path == filePath } ?: return

        dismissParseSheet()

        if (!fileUi.isSupportedForParsing) {
            parseResultUi.update {
                ParseResultUi(
                    title = "Неподдерживаемый файл",
                    message = "Сейчас парсинг работает только для PDF и PNG файлов.",
                    isMatch = false,
                )
            }
            return
        }

        val productName = getProductName().trim()
        if (productName.isBlank()) {
            parseResultUi.update {
                ParseResultUi(
                    title = "Нет имени продукта",
                    message = "Заполни название продукта на вкладке основных данных и повтори парсинг.",
                    isMatch = false,
                )
            }
            return
        }

        parseDeclarationFile(
            declarationId = sheetUi.declarationId,
            declarationName = sheetUi.declarationName,
            fileUi = fileUi,
            productName = productName,
        )
    }

    /**
     * Запускает проверку выбранного файла в фоне, показывает progress UI и после
     * завершения обновляет флаг [ProductDeclarationIn.isProductInDeclaration]
     * для конкретной декларации.
     */
    private fun parseDeclarationFile(
        declarationId: Int,
        declarationName: String,
        fileUi: ParseFileUi,
        productName: String,
    ) {
        parseProgressUi.update {
            ParseProgressUi(
                declarationId = declarationId,
                declarationName = declarationName,
                fileName = fileUi.name,
            )
        }

        coroutineScope.launch {
            runCatching {
                parser.parse(fileUi.path, productName)
            }.onSuccess { result ->
                productDeclarations.update { declarations ->
                    declarations.map { declaration ->
                        if (declaration.declarationId == declarationId) {
                            declaration.copy(isProductInDeclaration = result.isMatch)
                        } else {
                            declaration
                        }
                    }
                }

                parseResultUi.update {
                    ParseResultUi(
                        title = "Парсинг завершен",
                        message = if (result.isMatch) {
                            "Имя продукта \"$productName\" найдено в декларации ${declarationName}."
                        } else {
                            "Имя продукта \"$productName\" не найдено в декларации ${declarationName}."
                        },
                        isMatch = result.isMatch,
                    )
                }
            }.onFailure { throwable ->
                parseResultUi.update {
                    ParseResultUi(
                        title = "Ошибка парсинга",
                        message = throwable.message ?: "Не удалось распарсить выбранный файл.",
                        isMatch = false,
                    )
                }
            }

            parseProgressUi.update { null }
        }
    }

    /**
     * Открывает bottom sheet со списком файлов выбранной декларации.
     *
     * Здесь sheet реализован как локальное состояние компонента, а не как
     * decompose child, потому что это короткоживущий UI без отдельной бизнес-
     * логики: нужно просто показать список файлов и вернуть выбранный путь.
     */
    private fun openParseSheet(declarationId: Int) {
        coroutineScope.launch {
            val declarationUi = tableData.value.displayedItems.firstOrNull { it.declarationId == declarationId }
            val files = repository.getDeclarationFiles(declarationId)
            parseSheetUi.update {
                ParseDeclarationSheetUi(
                    declarationId = declarationId,
                    declarationName = declarationUi?.declarationName ?: "Декларация",
                    files = files.map(FileBD::toParseFileUi),
                )
            }

            if (files.isEmpty()) {
                parseSheetUi.update { null }
                parseResultUi.update {
                    ParseResultUi(
                        title = "Нет файлов",
                        message = "У выбранной декларации нет прикрепленных файлов.",
                        isMatch = false,
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun onEvent(event: ProductDeclarationEvent) {
        when (event) {

            ProductDeclarationEvent.AddNew -> {
                showDialog()
            }

            is ProductDeclarationEvent.DeleteSelected -> {
                productDeclarations.update { lst->
                    val updatedList = lst - lst.filter { it.declarationId in selectionManager.selectedIds }.toSet()
                    selectionManager.clearSelected()
                    updatedList
                }
            }
            is ProductDeclarationEvent.ToggleProductInDeclaration -> {
                productDeclarations.update { declarations ->
                    declarations.map { declaration ->
                        if (declaration.declarationId == event.declarationId) {
                            declaration.copy(isProductInDeclaration = event.isChecked)
                        } else {
                            declaration
                        }
                    }
                }
            }
            is ProductDeclarationEvent.ParseDeclaration -> {
                openParseSheet(event.declarationId)
            }
            is ProductDeclarationEvent.OpenDeclaration -> {tabOpener.openDeclarationTab(event.declarationId)}
            is ProductDeclarationEvent.Selection -> {
                selectionManager.onEvent(event.selectionUiEvent)
            }
        }
    }

    override val errorMessages: Flow<List<String>> =
        tableData.map { tableData ->
            val lst = tableData.displayedItems
            buildList {
                if (lst.isEmpty()) add("Добавьте хотя бы одну декларацию")
                if (lst.none { it.isActual }) add("Хотя бы одна декларация должна быть актуальной")
                if (lst.any { !it.isProductInDeclaration }) {
                    add("Отметьте, есть ли продукт в декларации")
                }
                if (lst.map { it.vendorName }
                        .toSet().size > 1) add("Все декларации должны быть от одного поставщика")
            }
        }
}

@Serializable
internal data object DeclarationDialogConfig

/**
 * События вкладки деклараций.
 *
 * Сюда входят как обычные действия таблицы, так и отдельные события для UI
 * парсинга декларации.
 */
internal sealed interface ProductDeclarationEvent {

    data class OpenDeclaration(val declarationId: Int) : ProductDeclarationEvent

    data class Selection(val selectionUiEvent: SelectionUiEvent) : ProductDeclarationEvent

    data object DeleteSelected : ProductDeclarationEvent

    data object AddNew : ProductDeclarationEvent

    data class ToggleProductInDeclaration(
        val declarationId: Int,
        val isChecked: Boolean,
    ) : ProductDeclarationEvent

    data class ParseDeclaration(val declarationId: Int) : ProductDeclarationEvent
}

/**
 * Состояние bottom sheet со списком файлов декларации.
 *
 * [onlyPdf] управляет локальной фильтрацией уже загруженного списка файлов,
 * оставляя только те, которые поддерживаются текущим парсером.
 */
internal data class ParseDeclarationSheetUi(
    val declarationId: Int,
    val declarationName: String,
    val files: List<ParseFileUi>,
    val onlyPdf: Boolean = true,
)

/**
 * UI-модель файла, доступного для парсинга.
 */
internal data class ParseFileUi(
    val name: String,
    val path: String,
    val isPdf: Boolean,
    val isPng: Boolean,
    val isSupportedForParsing: Boolean,
)

/**
 * Состояние диалога прогресса во время парсинга выбранной декларации.
 */
internal data class ParseProgressUi(
    val declarationId: Int,
    val declarationName: String,
    val fileName: String,
)

/**
 * Финальный результат парсинга, который показывается пользователю в диалоге.
 */
internal data class ParseResultUi(
    val title: String,
    val message: String,
    val isMatch: Boolean,
)

/**
 * Преобразует файл из БД в упрощенную UI-модель для списка в bottom sheet.
 */
private fun FileBD.toParseFileUi(): ParseFileUi {
    val file = java.io.File(path)
    val isPdf = file.extension.equals("pdf", ignoreCase = true)
    val isPng = file.extension.equals("png", ignoreCase = true)
    return ParseFileUi(
        name = file.name,
        path = path,
        isPdf = isPdf,
        isPng = isPng,
        isSupportedForParsing = isPdf || isPng,
    )
}
