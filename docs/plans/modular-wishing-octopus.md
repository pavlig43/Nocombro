# План исправления ProfitabilityDao

## Проблема

Ошибка компиляции KSP в `ProfitabilityDao.kt:29`:
```
The columns returned by the query does not have the properties [transactionId,count,batchId,movementId,productId,productName,vendorName,dateBorn,clientName,clientId]
in ru.pavlig43.database.`data`.analytic.profitability.InternalProfitabilityBD
```

**Корневая причина:**
`InternalProfitabilityBD` использует `@Embedded val sale: SaleBDOut`, но `SaleBDOut` - это DTO с denormalized данными, содержащий поля из нескольких таблиц (`productName`, `vendorName`, `dateBorn`, `clientName`), которых нет в результате SQL JOIN запроса.

## Решение

Следовать паттерну из `SaleDao.kt:38-59`:
1. Использовать `SaleBDIn` (Entity) вместо `SaleBDOut` (DTO) в `@Embedded`
2. Добавить `@Relation` для `Vendor` (client)
3. Создать функцию-маппер для преобразования `InternalProfitabilityBD`

## Изменения

**Файл:** `database/src/desktopMain/kotlin/ru/pavlig43/database/data/analytic/profitability/ProfitabilityDao.kt`

### 1. Обновить импорты
Добавить:
```kotlin
import ru.pavlig43.database.data.transact.sale.SaleBDIn
import ru.pavlig43.database.data.vendor.Vendor
```

### 2. Заменить `InternalProfitabilityBD`
```kotlin
internal data class InternalProfitabilityBD(
    @Embedded
    val sale: SaleBDIn,  // Было: SaleBDOut
    @Relation(
        entity = BatchMovement::class,
        parentColumn = "movement_id",  // Было: "transactionId"
        entityColumn = "id"
    )
    val movementOut: MovementOut,
    @Relation(
        entity = Vendor::class,
        parentColumn = "client_id",
        entityColumn = "id"
    )
    val client: Vendor  // Добавить
)
```

### 3. Добавить функцию-маппер
```kotlin
private fun InternalProfitabilityBD.toSaleBDOut(): SaleBDOut {
    val batchOut = movementOut.batchOut
    return SaleBDOut(
        transactionId = sale.transactionId,
        count = movementOut.movement.count,
        batchId = movementOut.movement.batchId,
        movementId = movementOut.movement.id,
        productId = batchOut.product.id,
        productName = batchOut.product.displayName,
        vendorName = batchOut.declaration.vendorName,
        dateBorn = batchOut.batch.dateBorn,
        clientName = client.displayName,
        clientId = client.id,
        price = sale.price,
        comment = sale.comment,
        id = sale.id
    )
}
```

### 4. Обновить использование в `observeOnProductSale`
```kotlin
fun observeOnProductSale(start: LocalDateTime, end: LocalDateTime): Flow<List<ProfitabilityBD>> {
    return observeOnSale(start, end).map { lst ->
        lst.groupBy { it.movementOut.batchOut.product.id }.values.mapParallel(Dispatchers.IO) { items ->
            items.map { prof ->
                val sale = prof.toSaleBDOut()  // Использовать маппер
                val contrAgentId = sale.clientId
                val contrAgentName = sale.clientName
                // ... остальной код
            }
        }
    }
}
```

## Проверка

1. Компиляция должна пройти без ошибок KSP
2. Запуск приложения для проверки функциональности
