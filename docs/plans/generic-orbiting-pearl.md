# План реализации таблицы прибыльности (Profitability)

## Обзор
Создание таблицы прибыльности по продукту (read-only), похожей на Storage, но без разворачивания партий. Таблица показывает финансовые показатели по каждому продукту за выбранный период.

## Структура данных

### ProfitabilityUi (уже существует)
```kotlin
data class ProfitabilityUi(
    val productId: Int,
    val productName: String,
    val quantity: Int,           // Количество проданного (граммы)
    val revenue: Int,            // Выручка (копейки)
    val expenses: Int,           // Расходы = 0 (по умолчанию)
    val expensesOnOneKg: Int,    // Расходы на 1 кг
    val profit: Int,             // Прибыль = revenue - expenses
    val margin: Double,          // Наценка %
    val profitability: Double    // Рентабельность %
)
```

## Файлы для создания/изменения

### 1. Database Layer

**Файл:** `database/src/desktopMain/kotlin/ru/pavlig43/database/data/money/profitability/ProfitabilityDao.kt`

Добавить метод для получения данных о продажах по продуктам:
```kotlin
@Query("""
    SELECT
        p.id as productId,
        p.displayName as productName,
        SUM(bm.count) as quantity,
        SUM(s.price * bm.count) as revenue
    FROM sale s
    INNER JOIN batch_movement bm ON s.movement_id = bm.id
    INNER JOIN batch bd ON bm.batch_id = bd.id
    INNER JOIN products p ON bd.product_id = p.id
    INNER JOIN transact t ON s.transaction_id = t.id
    WHERE t.created_at >= :start AND t.created_at <= :end
    GROUP BY p.id, p.displayName
""")
abstract fun observeProductSales(start: LocalDateTime, end: LocalDateTime): Flow<List<ProductSalesBD>>
```

**Создать файл:** `database/src/desktopMain/kotlin/ru/pavlig43/database/data/money/profitability/ProductSalesBD.kt`
```kotlin
data class ProductSalesBD(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val revenue: Int
)
```

### 2. Feature Layer (profitability)

**Файл:** `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/di/CreateModule.kt`

Завершить реализацию Repository:
```kotlin
internal fun createModule(dependencies: ProfitabilityDependencies) = listOf(
    module {
        single { dependencies.db }
        single { ProfitabilityRepository(get()) }
    }
)

class ProfitabilityRepository(
    db: NocombroDatabase
) : ImmutableListRepository<ProfitabilityUi> {
    private val dao = db.profitabilityDao

    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        // Не используется для readonly таблицы
        return Result.success(Unit)
    }

    override fun observeOnItems(parentId: Int): Flow<Result<List<ProfitabilityUi>>> {
        // Вернуть пустой список, так как данные будут браться из другого метода
        return flowOf(Result.success(emptyList()))
    }

    fun observeOnProducts(start: LocalDateTime, end: LocalDateTime): Flow<Result<List<ProfitabilityUi>>> {
        return dao.observeProductSales(start, end)
            .map { sales ->
                Result.success(sales.map { it.toUi() })
            }
            .catch { emit(Result.failure(it)) }
    }
}

private fun ProductSalesBD.toUi(): ProfitabilityUi {
    val expenses = 0  // По умолчанию
    val expensesOnOneKg = if (quantity > 0) expenses * 1000 / quantity else 0
    val profit = revenue - expenses
    val margin = if (expenses > 0) (profit.toDouble() / expenses * 100) else 0.0
    val profitability = if (revenue > 0) (profit.toDouble() / revenue * 100) else 0.0

    return ProfitabilityUi(
        productId = productId,
        productName = productName,
        quantity = quantity,
        revenue = revenue,
        expenses = expenses,
        expensesOnOneKg = expensesOnOneKg,
        profit = profit,
        margin = margin,
        profitability = profitability
    )
}
```

**Файл:** `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/component/ProfitabilityComponent.kt`

Обновить компонент:
```kotlin
class ProfitabilityComponent(
    componentContext: ComponentContext,
    dependencies: ProfitabilityDependencies
) : ComponentContext by componentContext, MainTabComponent {

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Прибыльность"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    val dateTimePeriodComponent = DateTimePeriodComponent(
        componentContext = childContext("date_time"),
        initDTPeriod = DTPeriod.thisMonth
    )

    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(createModule(dependencies))
    private val repository: ProfitabilityRepository = scope.get()

    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<ProfitabilityField>(childContext("filter"))
    private val sortManager = SortManager<ProfitabilityField>(childContext("sort"))

    internal val loadState: StateFlow<LoadState> = dateTimePeriodComponent.dateTimePeriodForData
        .transformLatest { dateTimePeriod ->
            emit(LoadState.Loading)
            repository.observeOnProducts(
                start = dateTimePeriod.start,
                end = dateTimePeriod.end
            ).collect { result ->
                emit(
                    result.fold(
                        onSuccess = { LoadState.Success(it) },
                        onFailure = { LoadState.Error(it.message ?: "") }
                    )
                )
            }
        }
        .stateIn(coroutineScope, SharingStarted.Lazily, LoadState.Loading)

    internal val tableData: StateFlow<ProfitabilityTableData> = combine(
        loadState,
        filterManager.filters,
        sortManager.sort
    ) { state, filters, sort ->
        when (state) {
            is LoadState.Loading, is LoadState.Error -> ProfitabilityTableData()
            is LoadState.Success -> {
                val filtered = state.data.filter { item ->
                    ProfitabilityFilterMatcher.matchesItem(item, filters)
                }
                val displayed = ProfitabilitySorter.sort(filtered, sort)
                ProfitabilityTableData(displayedProducts = displayed)
            }
        }
    }.stateIn(coroutineScope, SharingStarted.Lazily, ProfitabilityTableData())

    fun updateFilters(filters: Map<ProfitabilityField, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    fun updateSort(sort: SortState<ProfitabilityField>?) {
        sortManager.update(sort)
    }
}

internal sealed interface LoadState {
    data object Loading : LoadState
    data class Error(val message: String) : LoadState
    data class Success(val data: List<ProfitabilityUi>) : LoadState
}
```

**Создать файл:** `features/money/profitability/src/desktopMain/kotlin/ru/ppavlig43/profitability/internal/model/ProfitabilityTableData.kt`
```kotlin
@Immutable
internal data class ProfitabilityTableData(
    val displayedProducts: List<ProfitabilityUi> = emptyList()
)
```

**Обновить файл:** `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/model/ProfitabilityUi.kt`

Добавить реализацию IMultiLineTableUi:
```kotlin
data class ProfitabilityUi(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val revenue: Int,
    val expenses: Int,
    val expensesOnOneKg: Int,
    val profit: Int,
    val margin: Double,
    val profitability: Double,
    override val composeId: Int = productId
) : IMultiLineTableUi
```

**Файл:** `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/component/Columns.kt`

Создать колонки:
```kotlin
enum class ProfitabilityField {
    SELECTION,
    NAME,
    QUANTITY,
    REVENUE,
    EXPENSES,
    EXPENSES_ON_ONE_KG,
    PROFIT,
    MARGIN,
    PROFITABILITY
}

internal fun createProfitabilityColumns(
    onEvent: (ImmutableTableUiEvent) -> Unit
): ImmutableList<ColumnSpec<ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>> =
    tableColumns {
        idWithSelection(
            selectionKey = ProfitabilityField.SELECTION,
            idKey = ProfitabilityField.NAME,  // Используем NAME как ключ
            onEvent = onEvent
        )

        readTextColumn(
            headerText = "Продукт",
            column = ProfitabilityField.NAME,
            valueOf = { it.productName },
            filterType = TableFilterType.TextTableFilter()
        )

        readDecimalColumn(
            headerText = "Кол-во (кг)",
            column = ProfitabilityField.QUANTITY,
            valueOf = { DecimalData3(it.quantity) },
            filterType = TableFilterType.NumberTableFilter()
        )

        readDecimalColumn(
            headerText = "Выручка (₽)",
            column = ProfitabilityField.REVENUE,
            valueOf = { DecimalData2(it.revenue) },
            filterType = TableFilterType.NumberTableFilter()
        )

        readDecimalColumn(
            headerText = "Расходы (₽)",
            column = ProfitabilityField.EXPENSES,
            valueOf = { DecimalData2(it.expenses) },
            filterType = TableFilterType.NumberTableFilter()
        )

        readDecimalColumn(
            headerText = "Расходы/кг (₽)",
            column = ProfitabilityField.EXPENSES_ON_ONE_KG,
            valueOf = { DecimalData2(it.expensesOnOneKg) },
            filterType = TableFilterType.NumberTableFilter()
        )

        readDecimalColumn(
            headerText = "Прибыль (₽)",
            column = ProfitabilityField.PROFIT,
            valueOf = { DecimalData2(it.profit) },
            filterType = TableFilterType.NumberTableFilter()
        )

        readDecimalColumn(
            headerText = "Наценка (%)",
            column = ProfitabilityField.MARGIN,
            valueOf = { DecimalData2((it.margin * 100).toInt()) },
            filterType = TableFilterType.NumberTableFilter()
        )

        readDecimalColumn(
            headerText = "Рентабельность (%)",
            column = ProfitabilityField.PROFITABILITY,
            valueOf = { DecimalData2((it.profitability * 100).toInt()) },
            filterType = TableFilterType.NumberTableFilter()
        )
    }
```

**Создать файл:** `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/component/ProfitabilityFilterMatcher.kt`
```kotlin
internal object ProfitabilityFilterMatcher : FilterMatcher<ProfitabilityUi, ProfitabilityField>() {
    override fun matchesRules(
        item: ProfitabilityUi,
        column: ProfitabilityField,
        state: TableFilterState<*>
    ): Boolean {
        return when (column) {
            ProfitabilityField.SELECTION -> true
            ProfitabilityField.NAME -> matchesTextField(item.productName, state)
            else -> matchesNumberField(
                when (column) {
                    ProfitabilityField.QUANTITY -> DecimalData3(item.quantity)
                    ProfitabilityField.REVENUE -> DecimalData2(item.revenue)
                    ProfitabilityField.EXPENSES -> DecimalData2(item.expenses)
                    ProfitabilityField.EXPENSES_ON_ONE_KG -> DecimalData2(item.expensesOnOneKg)
                    ProfitabilityField.PROFIT -> DecimalData2(item.profit)
                    ProfitabilityField.MARGIN -> DecimalData2((item.margin * 100).toInt())
                    ProfitabilityField.PROFITABILITY -> DecimalData2((item.profitability * 100).toInt())
                    else -> return true
                },
                state
            )
        }
    }
}
```

**Создать файл:** `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/component/ProfitabilitySorter.kt`
```kotlin
internal object ProfitabilitySorter : SortMatcher<ProfitabilityUi, ProfitabilityField> {
    override fun sort(
        items: List<ProfitabilityUi>,
        sort: SortState<ProfitabilityField>?
    ): List<ProfitabilityUi> {
        if (sort == null) return items

        val sorted = when (sort.column) {
            ProfitabilityField.NAME -> items.sortedBy { it.productName }
            ProfitabilityField.QUANTITY -> items.sortedBy { it.quantity }
            ProfitabilityField.REVENUE -> items.sortedBy { it.revenue }
            ProfitabilityField.EXPENSES -> items.sortedBy { it.expenses }
            ProfitabilityField.PROFIT -> items.sortedBy { it.profit }
            else -> items
        }
        return if (sort.order == SortOrder.DESCENDING) sorted.asReversed() else sorted
    }
}
```

**Создать файл:** `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/api/ui/ProfitabilityScreen.kt`
```kotlin
@Composable
fun ProfitabilityScreen(component: ProfitabilityComponent) {
    DateTimeSelectorScreen(component.dateTimePeriodComponent)

    val loadState by component.loadState.collectAsState()
    when (val state = loadState) {
        is LoadState.Error -> ErrorScreen(state.message)
        is LoadState.Loading -> LoadingUi()
        is LoadState.Success -> {
            val columns = remember { createProfitabilityColumns(/* onEvent */) }
            val tableSettings = remember {
                TableSettings(showActiveFiltersHeader = true)
            }
            val tableState = rememberTableState(
                columns = ProfitabilityField.entries.toImmutableList(),
                settings = tableSettings,
            )
            LaunchedEffect(tableState) {
                snapshotFlow { tableState.filters.toMap() }.collect { filters ->
                    component.updateFilters(filters)
                }
            }
            val tableData by component.tableData.collectAsState()
            val verticalState = rememberLazyListState()

            ProfitabilityTable(
                state = tableState,
                tableData = tableData,
                columns = columns,
                verticalState = verticalState
            )
        }
    }
}
```

### 3. Integration Layer

**Файл:** `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt`

Добавить конфигурацию:
```kotlin
@Serializable
class ProfitabilityConfig : MainTabConfig
```

**Файл:** `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`

Добавить child:
```kotlin
class ProfitabilityChild(override val component: ProfitabilityComponent) : MainTabChild
```

**Файл:** `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabNavigationComponent.kt`

Добавить импорты и обработку конфигурации:
```kotlin
import ru.pavlig43.profitability.api.ProfitabilityDependencies
import ru.pavlig43.profitability.internal.component.ProfitabilityComponent
// ...

// В tabChildFactory:
is MainTabConfig.ProfitabilityConfig -> {
    ProfitabilityChild(
        ProfitabilityComponent(
            componentContext = context,
            dependencies = scope.get()
        )
    )
}
```

**Файл:** `features/money/main/src/desktopMain/kotlin/ru/pavlig43/money/main/api/component/MainComponent.kt`

Добавить TabOpener:
```kotlin
class MainComponent(
    componentContext: ComponentContext,
    private val onOpenTab: (MainTabConfig) -> Unit
) : ComponentContext by componentContext, MainTabComponent {
    // ...

    fun onProfitabilityClick() {
        onOpenTab(MainTabConfig.ProfitabilityConfig())
    }
}
```

**Файл:** `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabNavigationComponent.kt`

Обновить создание MainComponent:
```kotlin
is MainMoneyConfig -> MainMoneyChild(
    MainComponent(
        componentContext = context,
        onOpenTab = { config -> tabNavigationComponent.addTab(config) }
    )
)
```

### 4. DI Configuration

Добавить ProfitabilityDependencies в DI модуль (обычно в app/desktopApp или подобном месте).

## Проверка

1. Запустить приложение: `./gradlew :app:desktopApp:run`
2. Открыть "Деньги" → "Прибыльность"
3. Проверить, что таблица отображается с колонками:
   - Продукт
   - Кол-во (кг)
   - Выручка (₽)
   - Расходы (₽) = 0
   - Расходы/кг (₽)
   - Прибыль (₽)
   - Наценка (%)
   - Рентабельность (%)
4. Проверить фильтрацию по колонкам
5. Проверить сортировку
6. Проверить выбор периода

## Критические файлы для изменения

1. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/money/profitability/ProfitabilityDao.kt` - добавить SQL запрос
2. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/money/profitability/ProductSalesBD.kt` - создать (новый)
3. `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/di/CreateModule.kt` - завершить Repository
4. `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/component/ProfitabilityComponent.kt` - обновить
5. `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/model/ProfitabilityUi.kt` - добавить IMultiLineTableUi
6. `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/component/Columns.kt` - создать
7. `features/money/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/api/ui/ProfitabilityScreen.kt` - создать
8. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt` - добавить конфиг
9. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt` - добавить child
10. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabNavigationComponent.kt` - интеграция
11. `features/money/main/src/desktopMain/kotlin/ru/pavlig43/money/main/api/component/MainComponent.kt` - добавить onOpenTab
