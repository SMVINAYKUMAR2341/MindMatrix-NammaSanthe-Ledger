package com.nammasanthe.ledger.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.nammasanthe.ledger.data.entity.Customer
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.data.entity.TxnType
import com.nammasanthe.ledger.data.repo.AppProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoice {

    fun generate(
        context: Context,
        profile: AppProfile,
        customer: Customer,
        transactions: List<TxnEntity>
    ): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 @ 72dpi
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val title = Paint().apply {
            color = Color.parseColor("#E63946")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sub = Paint().apply { color = Color.DKGRAY; textSize = 11f }
        val text = Paint().apply { color = Color.BLACK; textSize = 12f }
        val bold = Paint(text).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val divider = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val accentBg = Paint().apply { color = Color.parseColor("#FFF1E6") }

        var y = 50f
        canvas.drawRect(0f, 0f, 595f, 90f, accentBg)
        canvas.drawText(profile.businessName.ifBlank { "Namma Santhe Ledger" }, 30f, y, title)
        y += 22f
        if (profile.ownerName.isNotBlank()) canvas.drawText(profile.ownerName, 30f, y, sub).also { y += 14f }
        if (profile.address.isNotBlank()) canvas.drawText(profile.address, 30f, y, sub).also { y += 14f }
        if (profile.phone.isNotBlank()) canvas.drawText("Phone: ${profile.phone}", 30f, y, sub).also { y += 14f }
        if (profile.gstNumber.isNotBlank()) canvas.drawText("GSTIN: ${profile.gstNumber}", 30f, y, sub).also { y += 14f }

        y = 130f
        canvas.drawText("INVOICE", 30f, y, bold); y += 18f
        val now = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Date: $now", 30f, y, text)
        canvas.drawText("Invoice #: INV-${System.currentTimeMillis()}", 350f, y, text)
        y += 24f

        canvas.drawText("Bill To:", 30f, y, bold); y += 14f
        canvas.drawText(customer.name, 30f, y, text); y += 14f
        if (customer.phone.isNotBlank()) canvas.drawText(customer.phone, 30f, y, text).also { y += 14f }

        y += 12f
        canvas.drawLine(30f, y, 565f, y, divider); y += 18f
        canvas.drawText("Date", 30f, y, bold)
        canvas.drawText("Type", 200f, y, bold)
        canvas.drawText("Note", 300f, y, bold)
        canvas.drawText("Amount", 480f, y, bold)
        y += 8f
        canvas.drawLine(30f, y, 565f, y, divider); y += 16f

        val df = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        var totalCredit = 0.0
        var totalPayment = 0.0
        for (t in transactions.sortedBy { it.date }) {
            if (y > 760f) break
            canvas.drawText(df.format(Date(t.date)), 30f, y, text)
            canvas.drawText(t.type.name, 200f, y, text)
            canvas.drawText((t.note ?: "—").take(28), 300f, y, text)
            val sign = if (t.type == TxnType.CREDIT) "+" else "-"
            canvas.drawText("$sign₹${"%.2f".format(t.amount)}", 480f, y, text)
            if (t.type == TxnType.CREDIT) totalCredit += t.amount else totalPayment += t.amount
            y += 16f
        }

        y += 10f
        canvas.drawLine(30f, y, 565f, y, divider); y += 18f
        canvas.drawText("Total Credit:", 350f, y, text)
        canvas.drawText("₹${"%.2f".format(totalCredit)}", 480f, y, text); y += 16f
        canvas.drawText("Total Payment:", 350f, y, text)
        canvas.drawText("₹${"%.2f".format(totalPayment)}", 480f, y, text); y += 18f
        val balance = totalCredit - totalPayment
        canvas.drawText("Balance Due:", 350f, y, bold)
        canvas.drawText("₹${"%.2f".format(balance)}", 480f, y, bold)

        canvas.drawText("Thank you for your business 🙏", 30f, 800f, sub)

        pdf.finishPage(page)

        val dir = File(context.filesDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${customer.id}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Invoice").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
