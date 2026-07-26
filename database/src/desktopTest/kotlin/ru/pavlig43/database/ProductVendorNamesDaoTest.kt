package ru.pavlig43.database

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.toVendorNamesText
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.scenario

class ProductVendorNamesDaoTest : DesktopMainDispatcherFunSpec({
    test(
        scenario(
            given = "products linked to current, expired and duplicate declarations",
            whenAction = "vendor names are read for product tables",
            thenResult = "all unique linked vendors are returned and unlinked products stay empty",
        )
    ) {
        withSeededTestDatabase { db ->
            db.productDao.create(
                Product(
                    type = ProductType.FOOD_BASE,
                    displayName = "Без поставщика",
                    createdAt = LocalDate(2026, 3, 1),
                    id = 99,
                )
            )
            db.productDeclarationDao.upsertProductDeclarations(
                listOf(
                    ProductDeclarationIn(productId = 1, declarationId = 2, id = 101),
                    ProductDeclarationIn(productId = 1, declarationId = 3, id = 102),
                    ProductDeclarationIn(productId = 1, declarationId = 1, id = 103),
                )
            )

            val selected = db.productDeclarationDao.getProductVendorNames(listOf(1, 99))
            selected.shouldHaveSize(3)
            selected.map { it.vendorName }.toVendorNamesText() shouldBe
                "Ингремарт, Рога и копыта, Стоинг"
            selected.none { it.productId == 99 } shouldBe true

            val observed = db.productDeclarationDao.observeAllProductVendorNames().first()
            observed.filter { it.productId == 1 }.shouldHaveSize(3)
        }
    }
})
