package ru.pavlig43.product.internal.update.tabs.specification

import ru.pavlig43.database.data.product.ProductSpecification

internal fun createDefaultProductSpecification(productId: Int): ProductSpecification {
    return ProductSpecification(
        productId = productId,
        dosage = "4 г на 1 кг фарша.",
        composition = "Комбинированная смесь для производства вареных колбас, сосисок, сарделек.",
        shelfLifeText = "18 месяцев.",
        storageConditions = "Хранить при температуре не выше 25°C и относительной влажности 80% в закрытой оригинальной упаковке.",
        appearance = "Порошок.",
        color = "",
        smell = "",
        taste = "",
        physicalChemicalIndicators = """
            Массовая доля влаги, %, не более: 7,0.
            Массовая доля металлических примесей, %, не более: 0,001.
            Посторонние примеси: Не допускается.
        """.trimIndent(),
        microbiologicalIndicators = """
            КМАФАнМ, КОЕ/г, не более: 500000.
            БГКП (колиформы), в 0,01 г: не допускается.
            Патогенные, в том числе сальмонеллы, в 25 г: не допускается.
            Сульфитредуцирующие клостридии, в 0,01 г: не допускается.
            Плесени, КОЕ/г, не более: 200.
        """.trimIndent(),
        toxicElements = """
            Свинец, мг/кг, не более: 5,0.
            Мышьяк, мг/кг, не более: 3,0.
            Кадмий, мг/кг, не более: 0,2.
        """.trimIndent(),
        allergens = encodeAllergensToJson(
            listOf(
                ProductSpecificationAllergenRow("Злаки, содержащие глютен, и их производные", "нет", "да"),
                ProductSpecificationAllergenRow("Горчица и ее производные", "нет", "да"),
                ProductSpecificationAllergenRow("Молоко и молочные продукты", "нет", "нет"),
                ProductSpecificationAllergenRow("Сельдерей и его производные", "нет", "да"),
                ProductSpecificationAllergenRow("Яйца и их производные", "нет", "нет"),
                ProductSpecificationAllergenRow("Кунжут - семена и производные", "нет", "нет"),
                ProductSpecificationAllergenRow("Ракообразные и продукты из них", "нет", "нет"),
                ProductSpecificationAllergenRow("Соевые бобы и продукты из них", "нет", "да"),
                ProductSpecificationAllergenRow("Орехи и продукты их переработки", "нет", "да"),
                ProductSpecificationAllergenRow("Арахис и продукты его переработки", "нет", "нет"),
                ProductSpecificationAllergenRow("Молюски и продукты их переработки", "нет", "нет"),
                ProductSpecificationAllergenRow("Люпин и продукты его переработки", "нет", "нет"),
            )
        ),
        gmoInfo = "Данный продукт не содержит генетически модифицированных объектов (ГМО), а также не производится из генетически модифицированных источников сырья.",
    )
}
