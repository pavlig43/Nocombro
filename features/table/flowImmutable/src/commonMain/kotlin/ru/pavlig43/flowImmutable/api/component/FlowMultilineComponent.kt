package ru.pavlig43.flowImmutable.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.flowImmutable.api.data.FlowMultilineRepository
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.tablecore.manger.FilterManager
import ru.pavlig43.tablecore.manger.SelectionManager
import ru.pavlig43.tablecore.manger.SortManager
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.utils.FilterMatcher
import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState

internal sealed interface ItemListState<out O> {
    data object Loading : ItemListState<Nothing>
    data class Success<O>(val data: List<O>) : ItemListState<O>
    data class Error(val message: String) : ItemListState<Nothing>
}
data class ObservableBDIn<BdIN:CollectionObject>(
    val composeId: Int,
    val bdIn: BdIN,
)

/**
 * Базовый компонент для управления таблицами с многострочными данными.
 *
 * **Строки таблицы подписаны на базу данных** и получают автоматические обновления
 * при изменении соответствующих записей в БД через Flow.
 *
 * Этот компонент обеспечивает:
 * - Загрузку начальных данных через [initDataComponent]
 * - **Реактивное отображение данных** - каждая строка подписывается на изменения
 *   в БД и автоматически обновляется при изменении соответствующей записи
 * - Управление выборкой строк (selection)
 * - Фильтрацию и сортировку данных
 * - Удаление выбранных элементов
 *
 * @param BdOUT Тип сущности для отображения (выходные данные из репозитория)
 * @param BdIN Тип сущности для сохранения (входные данные в репозиторий)
 * @param UI Тип данных для отображения в UI
 * @param Column Тип колонок таблицы
 *
 * @param componentContext Контекст компонента Decompose
 * @param parentId Идентификатор родительской сущности
 * @param getObservableId Функция получения идентификатора для отслеживания изменений
 * @param Mapper Функция преобразования сущности в UI модель
 * @param onRowClick Callback при клике на строку таблицы
 * @param filterMatcher Объект для матчинга фильтров
 * @param sortMatcher Объект для матчинга сортировки
 * @param repository Репозиторий для работы с данными
 */
@Suppress("LongParameterList")
abstract class FlowMultilineComponent<BdOUT: CollectionObject,BdIN:CollectionObject, UI : IMultiLineTableUi, Column>(
    componentContext: ComponentContext,
    parentId: Int,
    getObservableId: (BdIN) -> Int,
    mapper: BdOUT.(Int) -> UI,
    private val onRowClick: (UI) -> Unit,
    filterMatcher: FilterMatcher<UI, Column>,
    sortMatcher: SortMatcher<UI, Column>,
    private val repository: FlowMultilineRepository<BdOUT,BdIN>,

    ) : ComponentContext by componentContext, FormTabComponent {


    /**
     * Колонки таблицы для отображения.
     *
     * Должен быть переопределён в подклассах для определения структуры таблицы.
     */
    abstract val columns: ImmutableList<ColumnSpec<UI, Column, TableData<UI>>>


    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<Column>(childContext("filter"))
    private val sortManager = SortManager<Column>(childContext("sort"))
    private val selectionManager =
        SelectionManager(
            childContext("selection")
        )
    /**
     * Внутренний StateFlow для списка UI элементов.
     */
    private val _uiList = MutableStateFlow<List<UI>>(emptyList())

    /**
     * Публичный StateFlow для подписки на изменения списка UI элементов.
     * Используется для получения текущего списка элементов после применения фильтрации и сортировки.
     */
    protected val uiList = _uiList.asStateFlow()

    /**
     * StateFlow для хранения списка отслеживаемых входных сущностей.
     * Каждая сущность имеет уникальный [composeId] для идентификации в UI.
     */
    private val observableBDIn = MutableStateFlow<List<ObservableBDIn<BdIN>>>(emptyList())


    /**
     * Компонент для загрузки начальных данных.
     *
     * Загружает список [BdIN] сущностей для указанного [parentId]
     * и инициализирует [observableBDIn] с присвоением уникальных [composeId].
     */
    val initDataComponent = LoadInitDataComponent<List<BdIN>>(
        componentContext = childContext("init"),
        getInitData = {
            repository.getInit(parentId)
        },
        onSuccessGetInitData = { lst ->
            observableBDIn.update { lst.mapIndexed { index, bdIN -> ObservableBDIn(index + 1, bdIN) } }
        }
    )
    /**
     * StateFlow для отслеживания состояния загрузки элементов.
     *
     * Использует [flatMapLatest] для реактивного отслеживания изменений:
     * - При изменении [observableBDIn] отменяется предыдущая подписка и создаётся новая
     * - Репозиторий возвращает Flow с результатом или ошибкой
     * - Результат трансформируется в UI модели через [mapper]
     *
     * Возможные состояния:
     * - [ItemListState.Loading] - идёт загрузка
     * - [ItemListState.Success] - данные успешно загружены
     * - [ItemListState.Error] - произошла ошибка при загрузке
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val itemListState = observableBDIn.flatMapLatest { bdINS ->
        repository.observeOnItemsByIds(bdINS.map { getObservableId(it.bdIn) })
    }.map { result ->
        result.fold(
            onSuccess = { ItemListState.Success(it.mapIndexed { index, oUT -> mapper(oUT, index + 1) }) },
            onFailure = { ItemListState.Error(it.message ?: "unknown error") }
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        ItemListState.Loading
    )


    /**
     * Добавляет новую входную сущность в список отслеживаемых.
     *
     * Присваивает новой сущности уникальный [composeId] (максимальный существующий + 1)
     * и добавляет её в [observableBDIn].
     *
     * @param bdIn Входная сущность для добавления
     */
    protected fun addParentBD(bdIn: BdIN) {
        observableBDIn.update { lst ->
            val composeId = lst.maxOfOrNull { it.composeId }?.plus(1) ?: 1
            lst + ObservableBDIn(composeId, bdIn)
        }
    }

    /**
     * StateFlow с данными для отображения в таблице.
     *
     * Комбинирует несколько источников данных:
     * - [itemListState] - состояние загрузки элементов
     * - [selectionManager.selectedIdsFlow] - идентификаторы выбранных строк
     * - [filterManager.filters] - активные фильтры
     * - [sortManager.sort] - активная сортировка
     *
     * При изменении любого из источников автоматически:
     * 1. Применяет фильтрацию через [filterMatcher]
     * 2. Применяет сортировку через [sortMatcher]
     * 3. Обновляет [_uiList] текущим списком элементов
     * 4. Возвращает [TableData] с отображаемыми элементами
     */
    internal val tableData = combine(
        itemListState,
        selectionManager.selectedIdsFlow,
        filterManager.filters,
        sortManager.sort,
    ) { state, selectedIds, filters, sort ->
        when (state) {
            is ItemListState.Error,
            is ItemListState.Loading -> TableData(isSelectionMode = true)

            is ItemListState.Success -> {
                _uiList.update { state.data }
                val filtered = state.data.filter { ui ->
                    filterMatcher.matchesItem(ui, filters)
                }
                val displayed = sortMatcher.sort(filtered, sort)
                TableData(
                    displayedItems = displayed,
                    selectedIds = selectedIds,
                    isSelectionMode = true
                )

            }
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData(isSelectionMode = true)
    )

    /**
     * Обрабатывает события таблицы.
     *
     * Поддерживаемые события:
     * - [FlowMultiLineEvent.DeleteSelected] - удаляет выбранные элементы из списка
     * - [FlowMultiLineEvent.Selection] - delegates событие выбора в [selectionManager]
     * - [FlowMultiLineEvent.RowClick] - вызывает callback [onRowClick]
     *
     * @param event Событие для обработки
     */
    @Suppress("UNCHECKED_CAST")
    fun onEvent(event: FlowMultiLineEvent) {
        when (event) {

            is FlowMultiLineEvent.DeleteSelected -> {
                observableBDIn.update { lst ->
                    val updatedList = lst - lst.filter { it.composeId in selectionManager.selectedIds }.toSet()
                    selectionManager.clearSelected()
                    updatedList
                }
            }
            is FlowMultiLineEvent.Selection -> { selectionManager.onEvent(event.selectionUiEvent) }
            is FlowMultiLineEvent.RowClick<*> -> { onRowClick(event.item as UI) }
        }
    }
    /**
     * Сохраняет изменения в репозитории.
     *
     * Сравнивает начальные данные из [initDataComponent] с текущими данными из [observableBDIn]
     * и сохраняет изменения через [repository.update].
     *
     * @return Result успешности операции
     */
    override suspend fun onUpdate(): Result<Unit> {
        val old = initDataComponent.firstData.value
        val new = observableBDIn.value.map { it.bdIn }
        return repository.update(ChangeSet(old, new))
    }

    /**
     * Обновляет фильтры таблицы.
     *
     * @param filters Карта фильтров по колонкам
     */
    fun updateFilters(filters: Map<Column, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    /**
     * Обновляет состояние сортировки.
     *
     * Автоматически вызывает пересчёт данных через StateFlow комбинацию.
     *
     * @param sort Новое состояние сортировки (null для сброса)
     */
    fun updateSort(sort: SortState<Column>?) {
        sortManager.update(sort)
    }
}
