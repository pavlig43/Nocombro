package ru.pavlig43.database.data.files

const val PRODUCT_SPECIFICATION_FILE_NAME = "Спецификация.pdf"

fun isProductSpecificationFileName(
    fileName: String,
): Boolean {
    return fileName.trim().equals(PRODUCT_SPECIFICATION_FILE_NAME, ignoreCase = true)
}
