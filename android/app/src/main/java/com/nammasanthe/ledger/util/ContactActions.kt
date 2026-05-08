package com.nammasanthe.ledger.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ContactActions {

    private fun normalize(phone: String): String? {
        val digits = phone.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return if (digits.length == 10) "91$digits" else digits
    }

    fun call(context: Context, phone: String) {
        val number = normalize(phone) ?: run {
            Toast.makeText(context, "No phone number", Toast.LENGTH_SHORT).show(); return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun whatsappReminder(context: Context, name: String, phone: String, balance: Double) {
        val number = normalize(phone) ?: run {
            Toast.makeText(context, "No phone number", Toast.LENGTH_SHORT).show(); return
        }
        val msg = "Namaste $name, this is a friendly reminder about your pending balance of " +
            "₹${"%.2f".format(balance)}. Please clear it at your convenience. Dhanyavaad!"
        val url = "https://wa.me/$number?text=" + Uri.encode(msg)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage("com.whatsapp")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            // WhatsApp not installed — fall back to browser
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun whatsappBusinessReminder(context: Context, name: String, phone: String, balance: Double) {
        val number = normalize(phone) ?: return
        val msg = "Namaste $name, your pending balance: ₹${"%.2f".format(balance)}."
        val url = "https://wa.me/$number?text=" + Uri.encode(msg)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage("com.whatsapp.w4b")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Throwable) {
            whatsappReminder(context, name, phone, balance)
        }
    }
}
