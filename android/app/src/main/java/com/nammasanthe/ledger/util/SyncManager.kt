package com.nammasanthe.ledger.util

import android.util.Log
import com.nammasanthe.ledger.data.repo.LedgerRepository

/**
 * Optional Firebase sync. Disabled by default. The class is safe to instantiate
 * even without google-services.json — sync() just no-ops if Firebase isn't configured.
 */
class SyncManager(private val repository: LedgerRepository) {

    var enabled: Boolean = false

    suspend fun sync(): Result<Int> {
        if (!enabled) return Result.success(0)
        return try {
            val firestoreClass = Class.forName("com.google.firebase.firestore.FirebaseFirestore")
            val getInstance = firestoreClass.getMethod("getInstance")
            val firestore = getInstance.invoke(null)
            val pending = repository.pendingSyncTransactions()
            for (txn in pending) {
                val data = mapOf(
                    "id" to txn.id,
                    "customerId" to txn.customerId,
                    "type" to txn.type.name,
                    "amount" to txn.amount,
                    "note" to txn.note,
                    "date" to txn.date
                )
                val collection = firestoreClass.getMethod("collection", String::class.java)
                    .invoke(firestore, "transactions")
                val document = collection.javaClass.getMethod("document", String::class.java)
                    .invoke(collection, txn.id.toString())
                document.javaClass.getMethod("set", Any::class.java).invoke(document, data)
                repository.updateTransaction(txn.copy(syncPending = false))
            }
            Result.success(pending.size)
        } catch (t: Throwable) {
            Log.w("SyncManager", "sync skipped: ${t.message}")
            Result.success(0)
        }
    }
}
