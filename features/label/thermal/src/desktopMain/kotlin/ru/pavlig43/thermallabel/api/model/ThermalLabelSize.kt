package ru.pavlig43.thermallabel.api.model

enum class ThermalLabelSize(
    val title: String,
    internal val templateResourcePath: String,
    internal val outputSuffix: String,
) {
    SIZE_75_120(
        title = "75x120",
        templateResourcePath = "templates/75_120.pptx",
        outputSuffix = "75x120",
    ),
    SIZE_100_150(
        title = "100x150",
        templateResourcePath = "templates/100_150.pptx",
        outputSuffix = "100x150",
    ),
}
