package ru.pavlig43.product.internal.update.tabs.declaration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * Ищет имя продукта внутри файла декларации.
 *
 * Текущий формат деклараций в проекте часто сканированный, поэтому обычного
 * извлечения текстового слоя недостаточно. Из-за этого парсер приводит входной
 * файл к изображению страницы и затем прогоняет изображение через системный
 * Windows OCR.
 *
 * Поддерживаемые форматы:
 * 1. `pdf` - каждая страница рендерится в PNG через PDFBox;
 * 2. `png` - изображение сразу отдается в OCR без промежуточного рендера.
 *
 * После OCR текст нормализуется, чтобы сравнение было устойчивее к разнице в
 * регистре, пунктуации, пробелах и буквах `е/ё`.
 *
 * Ограничение реализации: OCR завязан на Windows Desktop, так как использует
 * WinRT OCR через PowerShell.
 */
internal class DeclarationProductPdfParser {

    /**
     * Возвращает признак того, найдено ли [productName] в поддерживаемом файле
     * по пути [filePath].
     */
    suspend fun parse(filePath: String, productName: String): ProductNameMatchResult {
        return withContext(Dispatchers.IO) {
            val extension = File(filePath).extension.lowercase()
            require(extension == "pdf" || extension == "png") {
                "Поддерживаются только PDF и PNG файлы"
            }
            require(productName.isNotBlank()) {
                "У продукта не заполнено название"
            }

            val normalizedProductName = normalizeForSearch(productName)
            val compactProductName = compactForSearch(productName)

            when (extension) {
                "pdf" -> parsePdf(
                    pdfPath = filePath,
                    normalizedProductName = normalizedProductName,
                    compactProductName = compactProductName,
                )

                "png" -> parsePng(
                    imagePath = filePath,
                    normalizedProductName = normalizedProductName,
                    compactProductName = compactProductName,
                )

                else -> error("Неподдерживаемое расширение файла: $extension")
            }
        }
    }

    private fun parsePdf(
        pdfPath: String,
        normalizedProductName: String,
        compactProductName: String,
    ): ProductNameMatchResult {
        Loader.loadPDF(File(pdfPath)).use { document ->
            val renderer = PDFRenderer(document)

            for (pageIndex in 0 until document.numberOfPages) {
                // 200 DPI дает достаточно четкую картинку для OCR без слишком
                // тяжелых временных файлов и заметного замедления.
                val image = renderer.renderImageWithDPI(pageIndex, 200f, ImageType.RGB)
                val pageImageFile = Files.createTempFile("nocombro-declaration-$pageIndex", ".png").toFile()

                try {
                    ImageIO.write(image, "png", pageImageFile)
                    val pageText = readTextWithWindowsOcr(pageImageFile.absolutePath)
                    if (containsProductName(pageText, normalizedProductName, compactProductName)) {
                        return ProductNameMatchResult(
                            isMatch = true,
                            extractedText = pageText,
                        )
                    }
                } finally {
                    pageImageFile.delete()
                }
            }
        }

        return ProductNameMatchResult(isMatch = false)
    }

    /**
     * Для PNG отдельный рендер не нужен: можно сразу отдать изображение в OCR.
     */
    private fun parsePng(
        imagePath: String,
        normalizedProductName: String,
        compactProductName: String,
    ): ProductNameMatchResult {
        val pageText = readTextWithWindowsOcr(imagePath)
        return ProductNameMatchResult(
            isMatch = containsProductName(pageText, normalizedProductName, compactProductName),
            extractedText = pageText,
        )
    }

    /**
     * Вызывает системный Windows OCR для уже подготовленного изображения
     * страницы и возвращает распознанный текст.
     */
    private fun readTextWithWindowsOcr(imagePath: String): String {
        // OCR вызывается через временный .ps1, потому что WinRT OCR проще и
        // стабильнее поднять из PowerShell, чем напрямую из desktop JVM.
        val scriptFile = Files.createTempFile("nocombro-windows-ocr", ".ps1").toFile()
        try {
            scriptFile.writeText(WINDOWS_OCR_SCRIPT, StandardCharsets.UTF_8)
            val process = ProcessBuilder(
                "powershell",
                "-STA",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                scriptFile.absolutePath,
                imagePath,
            )
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
            val exitCode = process.waitFor()
            check(exitCode == 0) {
                output.ifBlank { "OCR завершился с кодом $exitCode" }
            }
            return output
        } finally {
            scriptFile.delete()
        }
    }

    /**
     * Сравнивает имя продукта с OCR-результатом в двух режимах:
     * обычном и "сжатом" без пробелов.
     *
     * Это помогает пережить типичные OCR-ошибки, когда пробелы распознаны
     * нестабильно или слово разорвано.
     */
    private fun containsProductName(
        sourceText: String,
        normalizedProductName: String,
        compactProductName: String,
    ): Boolean {
        val normalizedSource = normalizeForSearch(sourceText)
        val compactSource = compactForSearch(sourceText)
        return normalizedSource.contains(normalizedProductName) || compactSource.contains(compactProductName)
    }

    /**
     * Нормализует текст для поиска: приводит к нижнему регистру, заменяет `ё`
     * на `е`, убирает знаки пунктуации и схлопывает повторяющиеся пробелы.
     */
    private fun normalizeForSearch(text: String): String {
        return text
            .lowercase()
            .replace('ё', 'е')
            .replace(NON_LETTER_OR_DIGIT_REGEX, " ")
            .replace(MULTIPLE_SPACES_REGEX, " ")
            .trim()
    }

    /**
     * Дополнительная форма нормализации без пробелов для поиска по OCR-тексту,
     * где расстояния между словами могут быть распознаны неточно.
     */
    private fun compactForSearch(text: String): String {
        return normalizeForSearch(text).replace(" ", "")
    }

    private companion object {
        /**
         * Удаляет все, кроме букв и цифр, чтобы сравнение не ломалось на
         * запятых, кавычках, дефисах и прочих OCR-артефактах.
         */
        val NON_LETTER_OR_DIGIT_REGEX = Regex("[^\\p{L}\\p{Nd}]+")

        /**
         * Схлопывает последовательности пробельных символов в один пробел.
         */
        val MULTIPLE_SPACES_REGEX = Regex("\\s+")

        /**
         * PowerShell-скрипт для вызова системного Windows OCR.
         *
         * Почему он хранится строкой:
         * - JVM-часть отвечает за PDF и подготовку картинок;
         * - PowerShell-часть использует WinRT OCR, который уже есть в Windows.
         *
         * Почему здесь везде `${'$'}`:
         * - `$` нужен самому PowerShell для переменных (`$file`, `$result`);
         * - но внутри Kotlin string template символ `$` трактуется как начало
         *   интерполяции Kotlin;
         * - поэтому `${'$'}` означает "положи в итоговую строку обычный символ
         *   доллара, не пытайся интерполировать Kotlin-переменную".
         *
         * Что делает скрипт:
         * 1. подключает WinRT-типы для работы с файлами и OCR;
         * 2. через AwaitOp преобразует WinRT async-операции в синхронный вызов;
         * 3. открывает изображение страницы;
         * 4. распознает текст на русском языке;
         * 5. печатает распознанный текст в stdout, откуда его читает Kotlin.
         */
        val WINDOWS_OCR_SCRIPT = """
param([string]${'$'}ImagePath)

Add-Type -Path 'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\System.Runtime.WindowsRuntime.dll'
${'$'}null = [Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime]
${'$'}null = [Windows.Storage.Streams.IRandomAccessStream, Windows.Storage, ContentType = WindowsRuntime]
${'$'}null = [Windows.Media.Ocr.OcrEngine, Windows.Foundation, ContentType = WindowsRuntime]
${'$'}null = [Windows.Graphics.Imaging.BitmapDecoder, Windows.Foundation, ContentType = WindowsRuntime]
${'$'}null = [Windows.Graphics.Imaging.SoftwareBitmap, Windows.Foundation, ContentType = WindowsRuntime]
${'$'}null = [Windows.Globalization.Language, Windows.Foundation, ContentType = WindowsRuntime]

# WinRT API возвращает асинхронные операции, а дальше в Kotlin нам удобнее
# получить уже готовый результат, поэтому здесь есть мост "async -> sync".
function AwaitOp(${'$'}operation, [Type]${'$'}resultType) {
    ${'$'}method = [System.WindowsRuntimeSystemExtensions].GetMethods() |
        Where-Object { ${'$'}_.Name -eq 'AsTask' -and ${'$'}_.IsGenericMethod -and ${'$'}_.GetParameters().Count -eq 1 } |
        Select-Object -First 1
    ${'$'}genericMethod = ${'$'}method.MakeGenericMethod(${'$'}resultType)
    ${'$'}task = ${'$'}genericMethod.Invoke(${'$'}null, @(${'$'}operation))
    ${'$'}task.GetAwaiter().GetResult()
}

${'$'}file = AwaitOp ([Windows.Storage.StorageFile]::GetFileFromPathAsync(${'$'}ImagePath)) ([Windows.Storage.StorageFile])
${'$'}stream = AwaitOp (${'$'}file.OpenAsync([Windows.Storage.FileAccessMode]::Read)) ([Windows.Storage.Streams.IRandomAccessStream])
${'$'}decoder = AwaitOp ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync(${'$'}stream)) ([Windows.Graphics.Imaging.BitmapDecoder])
${'$'}bitmap = AwaitOp (${'$'}decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap])
${'$'}ocrEngine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromLanguage([Windows.Globalization.Language]::new('ru'))
if (${'$'}null -eq ${'$'}ocrEngine) {
    ${'$'}ocrEngine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromUserProfileLanguages()
}
${'$'}result = AwaitOp (${'$'}ocrEngine.RecognizeAsync(${'$'}bitmap)) ([Windows.Media.Ocr.OcrResult])
Write-Output ${'$'}result.Text
"""
    }
}

/**
 * Результат проверки имени продукта внутри декларации.
 */
internal data class ProductNameMatchResult(
    val isMatch: Boolean,
    val extractedText: String = "",
)
