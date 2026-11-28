package com.pusacoffee.pusatrack.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

object IntentHelper {

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
}
