# Fix: Добавление декларации из диалога не отображается в таблице

## Проблема

При выборе декларации из диалога она не отображается в таблице.

### Ожидаемое поведение
1. Пользователь открывает диалог выбора декларации
2. Выбирает декларацию
3. Декларация сразу появляется в таблице

### Фактическое поведение
Декларация не появляется в таблице.

## Диагностика

### Текущий поток данных

```
1. Dialog: onItemClick → ProductDeclarationIn(id=0, productId=X, declarationId=Y)
2. addParentBD(declarationIn) → observableBDIn.update { ... }
3. itemListState = observableBDIn.flatMapLatest { bdINS ->
       repository.observeOnItemsByIds(bdINS.map { getObservableId(it.bdIn) })
   }
4. Для id=0: SQL WHERE id IN (0) → пустой результат
5. Ничего не отображается
```

### Корень проблемы

**`itemListState` идёт в БД для получения данных**, но для новых элементов с `id=0` их там нет.

`observableBDIn` уже содержит все данные (и из БД, и новые в памяти), но `itemListState` игнорирует это и ходит в репозиторий.

### Конфликт типов ID

В `ProductDeclarationComponent`:
```kotlin
getObservableId = { it.declarationId }  // declaration_id
```

В DAO:
```kotlin
@Query("SELECT * FROM product_declaration WHERE id IN (:ids)")  // PRIMARY KEY id
```

Это разные колонки! Но это не основная проблема - основная в том, что `itemListState` вообще не должен ходить в БД.

## Решение

`itemListState` должен использовать данные из `observableBDIn` напрямую, а не ходить в репозиторий.

### Изменения в `FlowMultilineComponent.kt`

**Файл:** `features/table/flowImmutable/src/commonMain/kotlin/ru/pavlig43/flowImmutable/api/component/FlowMultilineComponent.kt`

**Строки 146-161:**

Было:
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
internal val itemListState = observableBDIn.flatMapLatest { bdINS ->
    println(bdINS)
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
```

Станет:
```kotlin
internal val itemListState = observableBDIn.map { bdINS ->
    ItemListState.Success(bdINS.mapIndexed { index, obs ->
        mapper(obs.bdIn, index + 1)
    })
}.stateIn(
    coroutineScope,
    SharingStarted.Eagerly,
    ItemListState.Loading
)
```

**Обоснование:**
- `observableBDIn` уже содержит все данные (загруженные из БД + добавленные пользователем)
- Не нужно ходить в репозиторий - данные уже есть
- Убираем зависимость от `getObservableId` для получения данных
- Проще, быстрее, работает с новыми элементами

### Упрощение интерфейса репозитория

Метод `observeOnItemsByIds` больше не используется в `FlowMultilineComponent`. Можно:
1. Оставить его для других use cases
2. Или удалить как неиспользуемый

## Файлы для изменения

1. `features/table/flowImmutable/src/commonMain/kotlin/ru/pavlig43/flowImmutable/api/component/FlowMultilineComponent.kt` (строки 146-161)

## Проверка

1. Открыть форму продукта
2. Перейти на вкладку "Декларации"
3. Нажать кнопку добавления декларации
4. Вырать декларацию из диалога
5. **Ожидаемый результат:** декларация появляется в таблице
6. Сохранить продукт
7. Перезагрузить форму
8. **Ожидаемый результат:** декларация сохранилась и отображается
