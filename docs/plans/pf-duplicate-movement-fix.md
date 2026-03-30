# План: Дублирование INCOMING движения в OPZS (PF)

## Проблема

При сохранении PF в OPZS-транзакции может произойти дублирование batch и batch_movement. В транзакции 9 было создано два INCOMING движения (batch 15 и batch 16) вместо обновления одного.

### Симптомы
- Два INCOMING движения в одной OPZS транзакции
- Batch-дубликат без себестоимости (cost_price_per_unit = NULL)
- Некорректные остатки по партиям

### Симптомы
- Два INCOMING движения в одной OPZS транзакции
- Batch-дубликат без себестоимости (cost_price_per_unit = NULL)
- Задвоенные ингредиенты (OUTGOING движения дублируются)
- Некорректные остатки по партиям
- Себестоимость batch-оригинала посчитана по двойным ингредиентам

### Воспроизведение
1. Создать новый OPZS отчёт
2. Заполнить PF, ингредиенты и сохранить
3. **Не закрывая вкладку**, изменить PF (например, количество)
4. Сохранить снова — создаётся дубликат вместо обновления

> Важно: баг воспроизводится только при изменении **без перезагрузки вкладки**. Если закрыть и снова открыть отчёт, `PfDao.getPf()` загрузит данные с корректными ID из БД, и обновление пройдёт нормально.

### Корневая причина

`PfUpdateRepository.update()` создаёт batch и movement через `createBatch()`/`createMovement()`, но **новые ID не возвращаются компоненту**. Компонент продолжает держать `batchId=0, movementId=0`.

При повторном сохранении `batchId=0, movementId=0` → снова вызывается `createBatch()`/`createMovement()` → дубликат.

```
Сохранение 1: PfUi(batchId=0, movementId=0) → создаёт batch 15, movement 15
Сохранение 2: PfUi(batchId=0, movementId=0) → создаёт batch 16, movement 21 (дубль!)
```

### Почему BUY не страдает

BUY загружает данные через `buyDao.getBuysWithDetails()` с @Relation, где ID уже проставлены из БД. А PF загружается через `PfDao.getPf()` — тоже с @Relation. Но при первом создании PF данных в БД ещё нет, поэтому `getPf()` возвращает null, и `initItem` остаётся `PfUi(batchId=0, movementId=0)`.

## Решение

### Вариант 1: Защитная проверка в PfUpdateRepository (рекомендуется)

Перед созданием нового batch/movement проверять, существует ли уже INCOMING движение для этой транзакции.

Файл: `features/form/transaction/src/desktopMain/kotlin/ru/pavlig43/transaction/internal/di/CreateTransactionFormModule.kt`

В `PfUpdateRepository.update()`:
```kotlin
// Если movementId == 0, искать существующее INCOMING движение
val existingMovement = if (pf.movementId == 0) {
    batchMovementDao.getByTransactionId(pf.transactionId)
        .firstOrNull { it.movement.movementType == MovementType.INCOMING }
} else null

val batchId = existingMovement?.movement?.batchId ?: pf.batchId
val movementId = existingMovement?.movement?.id ?: pf.movementId
```

### Вариант 2: Возврат ID из update()

Изменить интерфейс `UpdateSingleLineRepository.update()`, чтобы возвращать обновлённую сущность с новыми ID. Больше изменение, зато правильно.

### Вариант 3: Перезагрузка данных после сохранения

После `update()` перезагрузить PF данные через `PfDao.getPf()`, чтобы компонент получил корректные ID.
