package com.nammasanthe.ledger.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nammasanthe.ledger.data.entity.ConfirmationEntity
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.data.repo.ConfirmationRepository
import com.nammasanthe.ledger.data.repo.LedgerRepository
import com.nammasanthe.ledger.security.QrGenerator
import com.nammasanthe.ledger.security.QrPayload
import com.nammasanthe.ledger.security.QrValidator
import com.nammasanthe.ledger.security.TrustLevel
import com.nammasanthe.ledger.sync.FirebaseAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI state types ─────────────────────────────────────────────────────────────

/** State for the QR Display screen — now includes Firebase confirmation waiting. */
sealed class QrDisplayState {
    object Idle    : QrDisplayState()
    data class Ready(
        val payload   : QrPayload,
        val bitmap    : Bitmap,
        val expiresAt : Long
    ) : QrDisplayState()
    /** Waiting for customer to confirm via Firebase web page. */
    data class WaitingForConfirmation(
        val payload : QrPayload,
        val bitmap  : Bitmap
    ) : QrDisplayState()
    /** Customer confirmed via Firebase — badge earned! */
    data class ConfirmedViaFirebase(
        val trustLevel : TrustLevel,
        val payload    : QrPayload
    ) : QrDisplayState()
    /** Customer rejected via Firebase. */
    data class RejectedViaFirebase(val payload: QrPayload) : QrDisplayState()
    object Expired : QrDisplayState()
}

/** State for the QR Scanner / Confirm screen (legacy in-app scanner). */
sealed class ScanState {
    object Idle : ScanState()
    /** Decoded and validated — awaiting user action. */
    data class Decoded(val payload: QrPayload, val trustLevel: TrustLevel) : ScanState()
    /** User confirmed — written to DB. */
    data class Confirmed(val trustLevel: TrustLevel) : ScanState()
    /** Rejected, expired, tampered, or replayed. */
    data class Failed(val reason: String) : ScanState()
}

// ── ViewModel ──────────────────────────────────────────────────────────────────

/**
 * Orchestrates QR code generation, Firebase real-time confirmation listening,
 * camera scan decoding, anti-fraud validation, and confirmation persistence.
 *
 * [vendorDeviceId] — stable hardware ID of the merchant device.
 * [appContext]     — needed to access FirebaseAuthManager for userId.
 */
class ConfirmationViewModel(
    private val confirmRepo    : ConfirmationRepository,
    private val ledgerRepo     : LedgerRepository,
    private val vendorDeviceId : String,
    private val appContext     : Context
) : ViewModel() {

    private val TAG = "ConfirmationVM"
    private val firestore = Firebase.firestore
    private val authManager = FirebaseAuthManager.getInstance(appContext)

    // Active Firestore listener — cleaned up on ViewModel clear
    private var firestoreListener: ListenerRegistration? = null

    // ─── QR display ──────────────────────────────────────────────────────────

    private val _qrState = MutableStateFlow<QrDisplayState>(QrDisplayState.Idle)
    val qrState: StateFlow<QrDisplayState> = _qrState.asStateFlow()

    /**
     * Build a signed QR payload from [txn], render its Bitmap as a URL QR code,
     * and start listening for Firebase confirmation.
     */
    fun requestQrForTxn(txn: TxnEntity) = viewModelScope.launch(Dispatchers.Default) {
        val payload = QrGenerator.buildPayload(txn)

        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            // Firebase mode: QR contains a web URL
            val vendorName = authManager.currentUser.value?.displayName ?: "Namma Santhe Vendor"
            val url    = QrGenerator.buildConfirmUrl(payload, userId, vendorName)
            val bitmap = QrGenerator.generateBitmapFromUrl(url)

            _qrState.value = QrDisplayState.Ready(
                payload   = payload,
                bitmap    = bitmap,
                expiresAt = payload.expiresAt
            )

            // Start listening for confirmation from Firebase
            startFirestoreListener(payload.txnId, userId, payload)
        } else {
            // Fallback: legacy offline QR with JSON payload
            val bitmap = QrGenerator.generateBitmap(payload)
            _qrState.value = QrDisplayState.Ready(
                payload   = payload,
                bitmap    = bitmap,
                expiresAt = payload.expiresAt
            )
        }
    }

    /**
     * Load the transaction by [txnId] from the repository, then generate QR.
     * Silently no-ops if the transaction is not found.
     */
    fun requestQrForTxnId(txnId: Long) = viewModelScope.launch {
        val txn = ledgerRepo.getTransactionById(txnId) ?: return@launch
        requestQrForTxn(txn)
    }

    /**
     * Start a real-time Firestore listener that watches for customer confirmation.
     * The customer's web page writes to: users/{userId}/confirmations/{txnId}
     */
    private fun startFirestoreListener(txnId: String, userId: String, payload: QrPayload) {
        // Cancel any existing listener
        firestoreListener?.remove()

        val docRef = firestore.collection("users")
            .document(userId)
            .collection("confirmations")
            .document(txnId)

        firestoreListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore listener error", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val trustLevelStr = snapshot.getString("trustLevel") ?: "VERIFIED"
                
                if (trustLevelStr == "REJECTED") {
                    _qrState.value = QrDisplayState.RejectedViaFirebase(payload)
                } else {
                    val trustLevel = try {
                        TrustLevel.valueOf(trustLevelStr)
                    } catch (e: Exception) {
                        TrustLevel.VERIFIED
                    }

                    // Save confirmation locally
                    viewModelScope.launch {
                        confirmRepo.confirmFromFirestore(
                            txnId           = txnId.toLongOrNull() ?: 0L,
                            trustLevel      = trustLevel,
                            scannerDeviceId = "web-customer",
                            vendorDeviceId  = vendorDeviceId,
                            nonce           = payload.nonce
                        )
                    }

                    _qrState.value = QrDisplayState.ConfirmedViaFirebase(trustLevel, payload)
                }

                // Stop listening after confirmation received
                firestoreListener?.remove()
                firestoreListener = null
            }
        }
    }

    /** Called by the UI countdown when the 60-second window closes. */
    fun markQrExpired() {
        _qrState.value = QrDisplayState.Expired
        firestoreListener?.remove()
        firestoreListener = null
    }

    /** Reset QR state on back navigation. */
    fun clearQr() {
        _qrState.value = QrDisplayState.Idle
        firestoreListener?.remove()
        firestoreListener = null
    }

    // ─── Scan / validation (legacy in-app scanner) ──────────────────────────

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    /**
     * Full validation pipeline for a camera-decoded [rawJson].
     *
     * Steps:
     *  1. Parse JSON safely
     *  2. Expiry check
     *  3. Hash integrity check
     *  4. Nonce reuse check (DB lookup)
     *  5. Trust level determination (device + speed checks)
     *  6. Post [ScanState.Decoded] for user review
     */
    fun onQrScanned(rawJson: String, scannerDeviceId: String) = viewModelScope.launch {
        // 1 — parse
        val payload = QrValidator.parse(rawJson)
            ?: return@launch setFailed("Invalid QR code — cannot parse")

        // 2 & 3 — expiry + hash
        val structural = QrValidator.validateStructure(payload)
        if (structural is QrValidator.Result.Reject) {
            return@launch setFailed(structural.reason)
        }

        // 4 — nonce reuse (DB)
        if (confirmRepo.isNonceUsed(payload.nonce)) {
            return@launch setFailed("This QR code has already been used (replay detected)")
        }

        // 5 — trust
        val trust = QrValidator.determineTrustLevel(
            payload         = payload,
            scannerDeviceId = scannerDeviceId,
            vendorDeviceId  = vendorDeviceId
        )

        // 6 — surface to UI for confirmation
        _scanState.value = ScanState.Decoded(payload, trust)
    }

    /**
     * User tapped "Confirm" on the scanned transaction details.
     * Atomically claims the nonce and writes the immutable confirmation record.
     */
    fun confirmScan(
        payload         : QrPayload,
        trustLevel      : TrustLevel,
        scannerDeviceId : String
    ) = viewModelScope.launch {
        val txnId = payload.txnId.toLongOrNull()
            ?: return@launch setFailed("Malformed transaction ID in QR")

        val ok = confirmRepo.confirm(
            txnId           = txnId,
            payload         = payload,
            scannerDeviceId = scannerDeviceId,
            vendorDeviceId  = vendorDeviceId,
            trustLevel      = trustLevel
        )
        _scanState.value = if (ok) ScanState.Confirmed(trustLevel)
                           else     ScanState.Failed("Already confirmed — replay prevented")
    }

    /** User tapped "Reject" or navigated back without confirming. */
    fun rejectScan() { _scanState.value = ScanState.Idle }

    /** Reset scanner state (e.g. between scans). */
    fun clearScanState() { _scanState.value = ScanState.Idle }

    // ─── Observation ──────────────────────────────────────────────────────────

    /** Live [Flow] of the confirmation for a given [txnId]. Emits null if not yet confirmed. */
    fun observeConfirmation(txnId: Long): Flow<ConfirmationEntity?> =
        confirmRepo.observeConfirmation(txnId)

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
        firestoreListener = null
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun setFailed(reason: String) { _scanState.value = ScanState.Failed(reason) }
}
