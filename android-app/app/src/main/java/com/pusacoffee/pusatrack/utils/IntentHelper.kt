package com.pusacoffee.pusatrack.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object IntentHelper {

    const val MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /**
     * Sends plain text via WhatsApp (`com.whatsapp`) if installed,
     * with a safe fallback to Android's share chooser if WhatsApp is unavailable.
     */
    fun sendWhatsAppText(context: Context, message: String) {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            context.startActivity(whatsappIntent)
        } catch (_: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            try {
                context.startActivity(
                    Intent.createChooser(fallbackIntent, "Kirim pesanan lewat aplikasi")
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "Tidak ada aplikasi yang dapat mengirim pesan teks.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Opens Android Sharesheet to share a generated `.xlsx` report safely via FileProvider.
     */
    fun shareXlsxFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_XLSX
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan laporan Excel"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Tidak ada aplikasi yang dapat menerima file laporan.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Creates a destination file and FileProvider URI for capturing an ingredient photo via camera.
     */
    fun createCameraPhotoTarget(context: Context): Pair<File, Uri> {
        val dir = File(context.filesDir, "ingredient_photos").apply {
            if (!exists()) mkdirs()
        }
        val file = File(dir, "ingredient_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return file to uri
    }

    /**
     * Copies a gallery image URI into app-internal `filesDir/ingredient_photos/` and returns its absolute path.
     */
    fun copyImageUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "ingredient_photos").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(dir, "ingredient_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
