# План исправления предупреждения Room в ProfitabilityDao

## Описание

Исправить предупреждение компиляции Room в `ProfitabilityDao.kt` о неиспользуемых колонках.

## Файл для изменения

**Файл:** `database/src/desktopMain/kotlin/ru/pavlig43/database/data/analytic/profitability/ProfitabilityDao.kt`

## Текущий код

```kotlin
@Transaction
@Query(
    """
    SELECT * FROM sale s
    JOIN transact t ON s.transaction_id = t.id
    WHERE t.created_at >= :start AND t.created_at <= :end
    """
)
abstract fun observeOnSale(
    start: LocalDateTime,
    end: LocalDateTime
): Flow<List<InternalSale>>
```

## Изменение

Заменить JOIN-запрос на подзапрос, соответствующий паттерну `@Relation`:

```kotlin
@Transaction
@Query(
    """
    SELECT * FROM sale
    WHERE transaction_id IN (
        SELECT id FROM transact
        WHERE created_at >= :start AND created_at <= :end
    )
    """
)
abstract fun observeOnSale(
    start: LocalDateTime,
    end: LocalDateTime
): Flow<List<InternalSale>>
```

## Обоснование

1. **Следует правилам проекта** — `.claude/rules/database.md` предписывает использовать `@Relation` вместо JOIN
2. **Устраняет предупреждение** — Room будет получать только колонки из `sale`, а `transact` загрузится через `@Relation`
3. **Эквивалентный результат** — подзапрос возвращает те же данные

## Проверка

После изменения скомпилируйте проект — предупреждение должно исчезнуть:

```bash
./gradlew build --continue
```

Ожидается: предупреждение `QUERY_MISMATCH` в `ProfitabilityDao.kt:21` больше не появляется.
