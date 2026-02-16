package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.core.mapValues
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductDeclarationOut

/**
 * DAO для работы со связями продуктов и деклараций.
 *
 * Предоставляет методы для:
 * - CRUD операций над связями Product-Declaration
 * - Реактивного отслеживания изменений
 * - Генерации уведомлений о просроченных декларациях
 */
@Dao
abstract class ProductDeclarationDao {

    /**
     * Вставляет или обновляет список связей продуктов и деклараций.
     *
     * @param declaration Список связей для сохранения
     */
    @Upsert
    abstract suspend fun upsertProductDeclarations(declaration: List<ProductDeclarationIn>)

    /**
     * Получает список связей для указанного продукта.
     *
     * @param productId Идентификатор продукта
     * @return Список связей Product-Declaration
     */
    @Query("SELECT * FROM product_declaration WHERE product_id = :productId")
    abstract suspend fun getProductDeclarationIn(productId: Int): List<ProductDeclarationIn>

    /**
     * Создаёт Flow для отслеживания всех связей продуктов и деклараций.
     *
     * Flow автоматически отправляет новые данные при изменениях в БД.
     * Использует @Relation для загрузки связанных сущностей (Product и Declaration).
     *
     * @return Flow со списком внутренних представлений связей
     */
    @Query(
        """
        SELECT * FROM product_declaration
    """
    )
    @Transaction
    internal abstract fun observeOnProductDeclaration(): Flow<List<InternalProductDeclaration>>

    /**
     * Создаёт Flow для отслеживания связей с указанными идентификаторами.
     *
     * Flow автоматически отправляет новые данные при изменении
     * любой из отслеживаемых связей в БД.
     *
     * @param ids Список идентификаторов связей для отслеживания
     * @return Flow со списком внутренних представлений связей
     */
    @Query("SELECT * FROM product_declaration WHERE id IN (:ids)")
    @Transaction
    internal abstract fun observeOnProductDeclarationByIds(ids: List<Int>): Flow<List<InternalProductDeclaration>>

    /**
     * Создаёт Flow для отслеживания выходных данных связей по идентификаторам.
     *
     * Преобразует внутреннее представление в [ProductDeclarationOut]
     * с вычислением флага актуальности.
     *
     * @param ids Список идентификаторов связей для отслеживания
     * @return Flow со списком выходных данных
     */
    fun observeOnProductDeclarationOutByIds(ids: List<Int>): Flow<List<ProductDeclarationOut>> {
        return observeOnProductDeclarationByIds(ids).mapValues(InternalProductDeclaration::toProductDeclarationOut)
    }

    /**
     * Создаёт Flow для отслеживания всех выходных данных связей.
     *
     * Преобразует внутреннее представление в [ProductDeclarationOut]
     * с вычислением флага актуальности.
     *
     * @return Flow со списком всех выходных данных
     */
    fun observeOnProductDeclarationOut(): Flow<List<ProductDeclarationOut>> {
        return observeOnProductDeclaration().mapValues(InternalProductDeclaration::toProductDeclarationOut)
    }

    /**
     * Удаляет связи с указанными идентификаторами.
     *
     * @param ids Список идентификаторов связей для удаления
     */
    @Query("DELETE FROM product_declaration WHERE id IN (:ids)")
    abstract suspend fun deleteProductDeclarations(ids: List<Int>)

    /**
     * Создаёт Flow для отслеживания уведомлений о проблемных декларациях.
     *
     * Генерирует уведомления для:
     * - Продуктов без деклараций
     * - Продуктов, где все декларации просрочены
     *
     * @param observeOnAllProduct Функция для получения Flow всех продуктов
     * @return Flow со списком уведомлений
     */
    fun observeOnProductDeclarationNotification(
        observeOnAllProduct: () -> Flow<List<Product>>
    ): Flow<List<NotificationDTO>> {
        return combine(
            observeOnAllProduct(),
            observeOnProductDeclaration()
        ) { products, declarations ->
            // 1. ID продуктов С декларациями
            val productsWithDeclarations = declarations.map { it.product.id }.toSet()

            // 2. Продукты БЕЗ деклараций
            val withoutDeclarations = products
                .filter { it.id !in productsWithDeclarations }
                .map { NotificationDTO(it.id, "В продукте ${it.displayName} нет декларации") }

            // 3. Продукты, где ВСЕ декларации просрочены
            val allExpired = declarations
                .groupBy { it.product.id }
                .filterValues { group ->
                    group.all { it.declaration.bestBefore <= getCurrentLocalDate() }
                }
                .mapNotNull { (productId, _) ->
                    declarations.find { it.product.id == productId }?.let {
                        NotificationDTO(
                            it.product.id,
                            "В продукте ${it.product.displayName} нет свежей декларации"
                        )
                    }
                }

            // 4. ОБЪЕДИНЯЕМ ВСЕ
            withoutDeclarations + allExpired
        }
    }
}

/**
 * Внутреннее представление связи продукта и декларации с загруженными связанными сущностями.
 *
 * Использует @Relation для автоматической загрузки связанных Product и Declaration.
 *
 * @property productDeclaration Связь продукта и декларации
 * @property declaration Связанная декларация
 * @property product Связанный продукт
 */
internal data class InternalProductDeclaration(
    @Embedded
    val productDeclaration: ProductDeclarationIn,
    @Relation(
        parentColumn = "declaration_id",
        entityColumn = "id"
    )
    val declaration: Declaration,

    @Relation(
        parentColumn = "product_id",
        entityColumn = "id"
    )
    val product: Product
)

/**
 * Преобразует внутреннее представление в выходное DTO.
 *
 * Вычисляет флаг [isActual] на основе сравнения даты срока годности
 * с текущей локальной датой.
 *
 * @return [ProductDeclarationOut] с заполненными полями
 */
private fun InternalProductDeclaration.toProductDeclarationOut(): ProductDeclarationOut {
    return ProductDeclarationOut(
        id = productDeclaration.id,
        productId = productDeclaration.productId,
        declarationId = productDeclaration.declarationId,
        declarationName = declaration.displayName,
        vendorName = declaration.vendorName,
        isActual = declaration.bestBefore > getCurrentLocalDate(),
    )
}