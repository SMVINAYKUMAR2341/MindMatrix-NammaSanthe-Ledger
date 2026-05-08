package com.nammasanthe.ledger.ui.nav

object Routes {
    // ── Onboarding & Auth ────────────────────────────────────────────────────
    const val Onboarding    = "onboarding"
    const val Login         = "login"
    const val Signup        = "signup"

    // ── Main App ──────────────────────────────────────────────────────────────
    const val Home          = "home"
    const val Customers     = "customers"
    const val Customer      = "customer/{id}"
    fun customer(id: Long)  = "customer/$id"
    const val QuickEntry    = "quick_entry"
    const val Reports       = "reports"
    const val Scanner       = "scanner"
    const val ScanResult    = "scan_result"
    const val Profile       = "profile"
    const val Auth          = "auth"

    // ── QR Confirmation ──────────────────────────────────────────────────────
    const val QrDisplay     = "qr_display/{txnId}"
    fun qrDisplay(id: Long) = "qr_display/$id"
    const val QrScanConfirm = "qr_scan_confirm"

    // ── Settings ─────────────────────────────────────────────────────────────
    const val GeminiSettings = "gemini_settings"

    // ── Transaction Detail ──────────────────────────────────────────────────
    const val TransactionDetail = "transaction_detail/{txnId}"
    fun transactionDetail(txnId: Long) = "transaction_detail/$txnId"
}
