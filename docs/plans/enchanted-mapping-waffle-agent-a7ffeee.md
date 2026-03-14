# Архитектурная рекомендация: Разделение поведения для ImmutableTableComponent

## Анализ текущей ситуации

### Текущая архитектура
- `ImmutableTableComponentFactoryMain` принимает два callback'а:
  - `tabOpener: TabOpener` — для onCreate (кнопка "Создать")
  - `onItemClick: (IMultiLineTableUi) -> Unit` — для клика на строку
- В `ImmutableListBox` (строка 63): `onRowClick = { component.onItemClick(it) }`
- onCreate → всегда открывает вкладку
- onItemClick → может быть разным поведением

### Проблема
Нужно поддерживать два сценария:
1. **В таблицах (обычное использование)** — клик на строку → открыть вкладку
2. **В MBS (BottomSheet)** — клик на строку → выбрать элемент (не открывать вкладку)

При этом onCreate всегда должен открывать вкладку.

---

## Рекомендация: Sealed class для стратегии поведения

### Почему НЕ подходят другие подходы

#### ❌ Nullable onItemClick
```kotlin
// ПЛОХО
private val onItemClick: ((IMultiLineTableUi) -> Unit)?
```

**Проблемы:**
- Неявное поведение — null-safe check везде
- Невозможно понять из API, что значит null
- Не scalable для будущих поведений

#### ❌ Разные фабрики
```kotlin
// ПЛОХО
class ImmutableTableComponentFactoryMain(...)
class ImmutableTableComponentFactoryForMBS(...)
```

**Проблемы:**
- Дублирование кода
- Тяжело поддерживать
- Нарушает DRY принцип

#### ❌ Boolean флаг
```kotlin
// ПЛОХО
private val openTabOnRowClick: Boolean
```

**Проблемы:**
- Не расширяемо (только 2 сценария)
- Неявное поведение
- Нельзя добавить параметризацию

---

## ✅ Рекомендуемое решение: Sealed class стратегии

### Архитектура

```kotlin
// 1. Определим sealed class для стратегии поведения
sealed interface RowClickStrategy<in T : IMultiLineTableUi> {
    fun onClick(item: T)
}

// 2. Реализации для разных сценариев
data class OpenTabStrategy<T : IMultiLineTableUi>(
    private val tabOpener: TabOpener,
    private val tabOpenerFn: (Int) -> Unit
) : RowClickStrategy<T> {
    override fun onClick(item: T) {
        tabOpenerFn(item.composeId)
    }
}

data class SelectItemStrategy<T : IMultiLineTableUi>(
    private val onSelect: (T) -> Unit
) : RowClickStrategy<T> {
    override fun onClick(item: T) {
        onSelect(item)
    }
}

// 3. В фабрике меняем onItemClick на стратегию
class ImmutableTableComponentFactoryMain(
    componentContext: ComponentContext,
    dependencies: ImmutableTableDependencies,
    private val immutableTableBuilderData: ImmutableTableBuilderData<out IMultiLineTableUi>,
    private val tabOpener: TabOpener,
    private val rowClickStrategy: RowClickStrategy<IMultiLineTableUi>, // Вместо onItemClick
) : ComponentContext by componentContext, MainTabComponent {

    // В build():
    ProductTableComponent(
        // ...
        onItemClick = { item -> rowClickStrategy.onClick(item) },
        onCreate = { tabOpener.openProductTab(0) },
        // ...
    )
}
```

### Использование

**Для обычных таблиц:**
```kotlin
ImmutableTableComponentFactoryMain(
    // ...
    rowClickStrategy = OpenTabStrategy(
        tabOpener = tabOpener,
        tabOpenerFn = tabOpener::openProductTab
    )
)
```

**Для MBS:**
```kotlin
ImmutableTableComponentFactoryMain(
    // ...
    rowClickStrategy = SelectItemStrategy(
        onSelect = { item -> mbsResultSelector(item) }
    )
)
```

---

## Преимущества подхода

### 1. **Type Safety**
- Компилятор проверяет все кейсы
- Невозможно забыть обработать стратегию

### 2. **Расширяемость**
Легко добавить новые сценарии:
```kotlin
data class ConfirmAndOpenTabStrategy<T : IMultiLineTableUi>(
    private val tabOpener: TabOpener,
    private val confirmDialog: (T, () -> Unit) -> Unit
) : RowClickStrategy<T> {
    override fun onClick(item: T) {
        confirmDialog(item) { tabOpener.openTab(item.composeId) }
    }
}
```

### 3. **Явность**
- Из типа сразу понятно поведение
- Легко тестировать каждую стратегию отдельно

### 4. **Null Safety**
- Никаких null checks
- Чистый Kotlin code без `!!`

### 5. **Testability**
```kotlin
class OpenTabStrategyTest {
    @Test
    fun `onClick should open tab`() {
        val tabOpener = mockk<TabOpener>()
        val strategy = OpenTabStrategy<ProductTableUi>(
            tabOpener = tabOpener,
            tabOpenerFn = tabOpener::openProductTab
        )

        strategy.onClick(ProductTableUi(composeId = 123))

        verify { tabOpener.openProductTab(123) }
    }
}
```

---

## Альтернатива: Extension function (если не нужна параметризация)

Если стратегии очень простые и не требуют параметризации:

```kotlin
sealed interface RowClickStrategy<in T : IMultiLineTableUi> {
    fun onClick(item: T)

    companion object {
        fun <T : IMultiLineTableUi> openTab(
            tabOpener: TabOpener,
            tabOpenerFn: (Int) -> Unit
        ) = object : RowClickStrategy<T> {
            override fun onClick(item: T) = tabOpenerFn(item.composeId)
        }

        fun <T : IMultiLineTableUi> select(
            onSelect: (T) -> Unit
        ) = object : RowClickStrategy<T> {
            override fun onClick(item: T) = onSelect(item)
        }
    }
}

// Использование:
rowClickStrategy = RowClickStrategy.openTab(tabOpener, tabOpener::openProductTab)
```

---

## Рефакторинг: Пошаговый план

### Шаг 1: Создать sealed interface
```kotlin
// В новом файле: ru/pavlig43/immutable/api/component/RowClickStrategy.kt
sealed interface RowClickStrategy<in T : IMultiLineTableUi> {
    fun onClick(item: T)
}
```

### Шаг 2: Создать реализации стратегий
```kotlin
// Там же: data class OpenTabStrategy и SelectItemStrategy
```

### Шаг 3: Изменить ImmutableTableComponentFactoryMain
```kotlin
// Заменить onItemClick на rowClickStrategy
// В build() вызвать rowClickStrategy.onClick(item)
```

### Шаг 4: Обновить все места создания фабрики
```kotlin
// Найти все места создания ImmutableTableComponentFactoryMain
// Заменить onItemClick на соответствующую стратегию
```

### Шаг 5: Удалить старый параметр onItemClick
```kotlin
// После обновления всех использований
```

---

## Примеры обновления кода

### Было:
```kotlin
ImmutableTableComponentFactoryMain(
    componentContext = context,
    dependencies = dependencies,
    immutableTableBuilderData = ProductImmutableTableBuilder(...),
    tabOpener = tabOpener,
    onItemClick = { item -> tabOpener.openProductTab(item.composeId) }
)
```

### Стало:
```kotlin
ImmutableTableComponentFactoryMain(
    componentContext = context,
    dependencies = dependencies,
    immutableTableBuilderData = ProductImmutableTableBuilder(...),
    tabOpener = tabOpener,
    rowClickStrategy = OpenTabStrategy(
        tabOpener = tabOpener,
        tabOpenerFn = tabOpener::openProductTab
    )
)
```

---

## Дополнительные улучшения

### 1. Типизация tabOpenerFn
Можно сделать тип более строгим:

```kotlin
typealias TabOpenerFn = (Int) -> Unit

data class OpenTabStrategy<T : IMultiLineTableUi>(
    private val tabOpener: TabOpener,
    private val tabOpenerFn: TabOpenerFn
) : RowClickStrategy<T>
```

### 2. Factory методы для создания стратегий
```kotlin
object RowClickStrategies {
    fun <T : IMultiLineTableUi> openTab(
        tabOpener: TabOpener,
        tabOpenerFn: TabOpenerFn
    ): RowClickStrategy<T> = OpenTabStrategy(tabOpener, tabOpenerFn)

    fun <T : IMultiLineTableUi> select(
        onSelect: (T) -> Unit
    ): RowClickStrategy<T> = SelectItemStrategy(onSelect)
}

// Использование:
rowClickStrategy = RowClickStrategies.openTab(tabOpener, tabOpener::openProductTab)
```

### 3. Inline классы для оптимизации (если нужно)
```kotlin
@JvmInline
value class RowClickStrategy<T : IMultiLineTableUi>(
    private val onClick: (T) -> Unit
) {
    fun onClick(item: T) = onClick(item)
}
```

---

## Заключение

**Рекомендуемый подход:** Sealed class `RowClickStrategy<T>` с реализациями `OpenTabStrategy` и `SelectItemStrategy`.

Это решение:
- ✅ Следует Kotlin best practices
- ✅ Type-safe и null-safe
- ✅ Расширяемо для будущих сценариев
- ✅ Легко тестировать
- ✅ Минимальный бойлерплейт
- ✅ Явное поведение из типа

**Альтернатива:** Если strategies не требуют параметризации — использовать factory методы в `companion object`.
