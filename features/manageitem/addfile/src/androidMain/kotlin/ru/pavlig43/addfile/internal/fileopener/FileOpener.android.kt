package ru.pavlig43.addfile.internal.fileopener

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLConnection

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class FileOpener(
    private val context: Context
) {
    actual fun openFile(filePath: String) {
        val file = File(filePath)

        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + "provider",
            file

        )
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri,getMimeType(filePath))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(this)
        }
    }
    private fun getMimeType(filePath:String): String? {
        return URLConnection.guessContentTypeFromName(filePath)
    }
}