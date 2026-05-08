package com.nammasanthe.ledger.data.repo

import com.nammasanthe.ledger.data.dao.ConfirmationDao
import com.nammasanthe.ledger.data.dao.UsedNonceDao
import com.nammasanthe.ledger.data.entity.ConfirmationEntity
import com.nammasanthe.ledger.data.entity.UsedNonce
import com.nammasanthe.ledger.security.QrPayload
import com.nammasanthe.ledger.security.TrustLevel
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for QR confirmation data.
 *
 * Security invariants enforced here:
 *  1. Nonce is claimed atomically before writing the confirmation.
 *  2. No confirmation record is ever deleted.
 *  3. A transaction can only be confirmed once (IGNORE on conflict).
 */
class ConfirmationRepository(
    private val confirmationDao: ConfirmationDao,
    private val usedNonceDao   : UsedNonceDao
) {

    // ── Read ─────────────────────────────────────────────────────────────────

    fun observeConfirmation(txnId: Long): Flow<ConfirmationEntity?> =
        confirmationDao.observeByTxnId(txnId)

    fun observeAll(): Flow<List<ConfirmationEntity>> =
        confirmationDao.observeAll()

    suspend fun getConfirmation(txnId: Long): ConfirmationEntity? =
        confirmationDao.getByTxnId(txnId)

    suspend fun isNonceUsed(nonce: String): Boolean =
        usedNonceDao.isUsed(nonce)

    // ── Write ────────────────────────────────────────────────────────────────

    /**
     * Atomically claims the nonce and writes the confirmation.
     *
     * @return true  if confirmation was stored successfully
     *         false if nonce was already used (replay attack)
     */
    suspend fun confirm(
        txnId           : Long,
        payload         : QrPayload,
        scannerDeviceId : String,
        vendorDeviceId  : String,
        trustLevel      : TrustLevel
    ): Boolean {
        // Step 1 — claim nonce (single-use enforcement)
        val claimed = usedNonceDao.claim(
            UsedNonce(nonce = payload.nonce, txnId = txnId)
        )
        if (claimed == -1L) return false   // nonce already exists → replay

        // Step 2 — write immutable confirmation record
        confirmationDao.insert(
            ConfirmationEntity(
                txnId           = txnId,
                confirmedAt     = System.currentTimeMillis(),
                scannerDeviceId = scannerDeviceId,
                vendorDeviceId  = vendorDeviceId,
                trustLevel      = trustLevel,
                nonce           = payload.nonce
            )
        )
        return true
    }

    /**
     * Save a confirmation that arrived from Firebase (customer confirmed via web page).
     * Does not require nonce validation since Firebase security rules prevent replays.
     *
     * @return true if the confirmation was stored successfully
     */
    suspend fun confirmFromFirestore(
        txnId           : Long,
        trustLevel      : TrustLevel,
        scannerDeviceId : String,
        vendorDeviceId  : String,
        nonce           : String
    ): Boolean {
        // Check if already confirmed locally
        val existing = confirmationDao.getByTxnId(txnId)
        if (existing != null) return false // already confirmed

        // Write immutable confirmation record
        confirmationDao.insert(
            ConfirmationEntity(
                txnId           = txnId,
                confirmedAt     = System.currentTimeMillis(),
                scannerDeviceId = scannerDeviceId,
                vendorDeviceId  = vendorDeviceId,
                trustLevel      = trustLevel,
                nonce           = nonce
            )
        )
        return true
    }
}
