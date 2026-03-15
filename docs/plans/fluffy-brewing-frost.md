# План: Исправление StorageDao для соответствия правилам @Relation

## Проблема

`StorageDao.kt` использует raw SQL JOIN для фильтрации по связанным полям (`t.created_at`), но возвращает `MovementOut` с `@Relation`. Это архитектурно несовместимо:

**Текущий код (неправильно):**
```kotlin
@Transaction
@Query(
    """
    SELECT bm.* FROM batch_movement bm
    INNER JOIN transact t ON bm.transaction_id = t.id
    WHERE t.created_at <= :end
    """
)
internal abstract fun observeMovementsUntil(end: LocalDateTime): Flow<List<MovementOut>>
```

`MovementOut` использует `@Relation`:
```kotlin
internal data class MovementOut(
    @Embedded
    val movement: BatchMovement,
    @Relation(...) val batchOut: BatchOut,
    @Relation(...) val transaction: Transact
)
```

**Проблема:** `@Relation` требует `SELECT * FROM parent_table`, а JOIN добавляет лишние данные.

## Решение

### Вариант A: Убрать JOIN, фильтровать в Kotlin

**Плюсы:**
- Полное соответствие правилу database.md
- Чистый код с использованием `@Relation`
- Room автоматически кэширует связи

**Минусы:**
- Загружаются все транзакции, затем фильтрация в Kotlin
- Может быть медленно при больших данных

**Реализация:**
1. Создать метод в `BatchMovementDao` для получения всех движений с связями
2. В `StorageDao` фильтровать по дате в Kotlin через `.filter()`

### Вариант B: Создать отдельный DTO для JOIN запросов

**Плюсы:**
- Эффективная фильтрация в SQL
- Не загружаются лишние данные

**Минусы:**
- Дублирование DTO
- Не соответствует правилу database.md

**Реализация:**
1. Создать `MovementWithTransactFlat` без `@Relation`
2. Использовать только для `StorageDao`

## Рекомендуемый подход: Вариант A

Этот вариант полностью соответствует правилам проекта.

## Критические файлы

| Файл | Изменения |
|------|-----------|
| `database/src/.../storage/dao/StorageDao.kt` | Переписать методы без JOIN |
| `database/src/.../batch/dao/BatchMovementDao.kt` | Добавить метод `observeAllMovements()` |

## Проверка (Verification)

1. Проверить, что нет ошибок компиляции
2. Убедиться, что данные фильтруются корректно по дате
3. Проверить производительность (если нужно)
