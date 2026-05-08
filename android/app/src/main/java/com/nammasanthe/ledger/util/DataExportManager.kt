package com.nammasanthe.ledger.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.nammasanthe.ledger.data.entity.Customer
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.data.repo.AppProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages exporting complete ledger data as PDF.
 * Includes customers, transactions, summaries, and photo references.
 */
object DataExportManager {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    /**
     * Export complete ledger data as PDF.
     */
    suspend fun exportCompleteData(
        context: Context,
        profile: AppProfile,
        customers: List<Customer>,
        transactions: List<TxnEntity>,
        getCustomerName: (Long) -> String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
            pdfDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "NammaSanthe_Ledger_${profile.businessName}_${timestamp}.pdf"
            val pdfFile = File(pdfDir, fileName)

            PdfWriter(pdfFile.absolutePath).use { writer ->
                PdfDocument(writer).use { pdfDoc ->
                    Document(pdfDoc).use { document ->
                        // Header
                        addHeader(document, profile)

                        // Summary Section
                        addSummary(document, profile, customers, transactions)

                        // Customer List
                        addCustomerList(document, customers)

                        // All Transactions
                        addAllTransactions(document, transactions, getCustomerName)

                        // Footer
                        addFooter(document)
                    }
                }
            }

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share the exported PDF.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Namma Santhe Ledger Export")
            putExtra(Intent.EXTRA_TEXT, "Complete ledger data export from Namma Santhe App")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Ledger PDF"))
    }

    private fun addHeader(document: Document, profile: AppProfile) {
        // Title
        val title = Paragraph("NAMMA SANTHE LEDGER")
            .setFontSize(24f)
            .setBold()
            .setFontColor(DeviceRgb(234, 88, 12)) // Saffron color
            .setTextAlignment(TextAlignment.CENTER)
        document.add(title)

        // Business Name
        val businessName = profile.businessName.ifBlank { profile.ownerName }
        if (businessName.isNotBlank()) {
            document.add(
                Paragraph(businessName)
                    .setFontSize(18f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )
        }

        // Contact Info
        val contactInfo = buildString {
            if (profile.ownerName.isNotBlank()) append("Owner: ${profile.ownerName}\n")
            if (profile.phone.isNotBlank()) append("Phone: ${profile.phone}\n")
            if (profile.address.isNotBlank()) append("Address: ${profile.address}\n")
            if (profile.gstNumber.isNotBlank()) append("GST: ${profile.gstNumber}")
        }

        if (contactInfo.isNotBlank()) {
            document.add(
                Paragraph(contactInfo)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
        }

        document.add(Paragraph("Exported on: ${dateTimeFormat.format(Date())}")
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic()
        )

        document.add(Paragraph("\n"))
    }

    private fun addSummary(
        document: Document,
        profile: AppProfile,
        customers: List<Customer>,
        transactions: List<TxnEntity>
    ) {
        // Section Title
        document.add(
            Paragraph("SUMMARY")
                .setFontSize(14f)
                .setBold()
                .setBackgroundColor(DeviceRgb(254, 243, 199))
                .setPadding(8f)
        )

        // Calculate totals
        val totalCredit = transactions.filter { it.type.name == "CREDIT" }.sumOf { it.amount }
        val totalPayment = transactions.filter { it.type.name == "PAYMENT" }.sumOf { it.amount }
        val outstanding = totalCredit - totalPayment

        // Summary Table
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .useAllAvailableWidth()

        summaryTable.addCell(createSummaryCell("Total Customers:", customers.size.toString()))
        summaryTable.addCell(createSummaryCell("Total Transactions:", transactions.size.toString()))
        summaryTable.addCell(createSummaryCell("Total Credit (Udhar):", "₹${String.format("%.2f", totalCredit)}"))
        summaryTable.addCell(createSummaryCell("Total Payments:", "₹${String.format("%.2f", totalPayment)}"))

        // Outstanding with color
        val outstandingCell = createSummaryCell("Net Outstanding:", "₹${String.format("%.2f", outstanding)}")
        if (outstanding > 0) {
            outstandingCell.setFontColor(DeviceRgb(220, 38, 38)) // Red for pending
        } else {
            outstandingCell.setFontColor(DeviceRgb(34, 197, 94)) // Green for settled
        }
        summaryTable.addCell(outstandingCell)
        summaryTable.addCell(createSummaryCell("Business Language:", profile.language.uppercase()))

        document.add(summaryTable)
        document.add(Paragraph("\n"))
    }

    private fun createSummaryCell(label: String, value: String): Cell {
        return Cell()
            .add(Paragraph(label).setFontSize(11f).setBold())
            .add(Paragraph(value).setFontSize(12f))
            .setPadding(8f)
            .setBorder(Border.NO_BORDER)
    }

    private fun addCustomerList(document: Document, customers: List<Customer>) {
        if (customers.isEmpty()) return

        document.add(
            Paragraph("CUSTOMER LIST")
                .setFontSize(14f)
                .setBold()
                .setBackgroundColor(DeviceRgb(219, 234, 254))
                .setPadding(8f)
        )

        val table = Table(UnitValue.createPercentArray(floatArrayOf(8f, 35f, 25f, 20f, 12f)))
            .useAllAvailableWidth()

        // Header
        val headerColor = DeviceRgb(59, 130, 246)
        table.addHeaderCell(createHeaderCell("S.No", headerColor))
        table.addHeaderCell(createHeaderCell("Customer Name", headerColor))
        table.addHeaderCell(createHeaderCell("Phone", headerColor))
        table.addHeaderCell(createHeaderCell("Location", headerColor))
        table.addHeaderCell(createHeaderCell("Status", headerColor))

        // Data rows
        customers.forEachIndexed { index, customer ->
            table.addCell(createDataCell((index + 1).toString()))
            table.addCell(createDataCell(customer.name))
            table.addCell(createDataCell(customer.phone ?: "-"))
            table.addCell(createDataCell(customer.address.takeIf { it.isNotBlank() } ?: "-"))
            table.addCell(createDataCell("Active"))
        }

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addAllTransactions(
        document: Document,
        transactions: List<TxnEntity>,
        getCustomerName: (Long) -> String
    ) {
        if (transactions.isEmpty()) return

        document.add(
            Paragraph("ALL TRANSACTIONS")
                .setFontSize(14f)
                .setBold()
                .setBackgroundColor(DeviceRgb(220, 252, 231))
                .setPadding(8f)
        )

        // New page for transactions if many
        if (transactions.size > 30) {
            document.add(Paragraph("(Continued on next page...)"))
        }

        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 25f, 15f, 20f, 20f, 10f)))
            .useAllAvailableWidth()

        // Header
        val headerColor = DeviceRgb(34, 197, 94)
        table.addHeaderCell(createHeaderCell("Date", headerColor))
        table.addHeaderCell(createHeaderCell("Customer", headerColor))
        table.addHeaderCell(createHeaderCell("Type", headerColor))
        table.addHeaderCell(createHeaderCell("Amount", headerColor))
        table.addHeaderCell(createHeaderCell("Note", headerColor))
        table.addHeaderCell(createHeaderCell("Photo", headerColor))

        // Sort by date descending
        val sortedTransactions = transactions.sortedByDescending { it.date }

        sortedTransactions.forEach { txn ->
            table.addCell(createDataCell(dateFormat.format(Date(txn.date))))
            table.addCell(createDataCell(getCustomerName(txn.customerId)))

            // Type with color
            val typeCell = createDataCell(txn.type.name)
            if (txn.type.name == "CREDIT") {
                typeCell.setFontColor(DeviceRgb(220, 38, 38))
            } else {
                typeCell.setFontColor(DeviceRgb(34, 197, 94))
            }
            table.addCell(typeCell)

            table.addCell(createDataCell("₹${String.format("%.2f", txn.amount)}"))
            table.addCell(createDataCell(txn.note?.take(20) ?: "-"))
            table.addCell(createDataCell(if (txn.photoPath != null) "📸" else "-"))
        }

        document.add(table)
        document.add(Paragraph("\n"))

        // Transaction summary by customer
        document.add(
            Paragraph("CUSTOMER WISE OUTSTANDING")
                .setFontSize(12f)
                .setBold()
                .setBackgroundColor(DeviceRgb(254, 243, 199))
                .setPadding(6f)
        )

        val customerTotals = transactions.groupBy { it.customerId }.map { (customerId, txns) ->
            val credit = txns.filter { it.type.name == "CREDIT" }.sumOf { it.amount }
            val payment = txns.filter { it.type.name == "PAYMENT" }.sumOf { it.amount }
            Triple(customerId, getCustomerName(customerId), credit - payment)
        }.sortedByDescending { it.third }

        val outstandingTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            .useAllAvailableWidth()

        outstandingTable.addHeaderCell(createHeaderCell("Customer", DeviceRgb(234, 88, 12)))
        outstandingTable.addHeaderCell(createHeaderCell("Outstanding (₹)", DeviceRgb(234, 88, 12)))

        customerTotals.forEach { (_, name, outstanding) ->
            outstandingTable.addCell(createDataCell(name))
            val amountCell = createDataCell(String.format("%.2f", outstanding))
            if (outstanding > 0) {
                amountCell.setFontColor(DeviceRgb(220, 38, 38))
            }
            outstandingTable.addCell(amountCell)
        }

        document.add(outstandingTable)
    }

    private fun addFooter(document: Document) {
        document.add(Paragraph("\n\n"))

        val footer = Paragraph()
            .add("Generated by Namma Santhe Ledger App\n")
            .add("This is a computer generated statement and does not require signature.\n")
            .add("For disputes, refer to transaction photos and QR confirmations in app.")
            .setFontSize(9f)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic()
            .setFontColor(ColorConstants.GRAY)

        document.add(footer)
    }

    private fun createHeaderCell(text: String, bgColor: DeviceRgb): Cell {
        return Cell()
            .add(Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(bgColor)
            .setPadding(6f)
            .setTextAlignment(TextAlignment.CENTER)
    }

    private fun createDataCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text))
            .setPadding(5f)
            .setFontSize(9f)
    }
}
