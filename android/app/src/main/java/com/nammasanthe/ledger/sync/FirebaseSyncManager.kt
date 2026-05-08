package com.nammasanthe.ledger.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nammasanthe.ledger.data.entity.Customer
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.data.repo.AppProfile
import com.nammasanthe.ledger.util.PhotoProofManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages cloud sync with Firebase Firestore.
 * Works offline-first - queues changes and syncs when online.
 */
class FirebaseSyncManager private constructor(context: Context) {

    private val db: FirebaseFirestore = Firebase.firestore
    private val appContext = context.applicationContext
    private val authManager = FirebaseAuthManager.getInstance(context)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    companion object {
        @Volatile
        private var instance: FirebaseSyncManager? = null

        fun getInstance(context: Context): FirebaseSyncManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseSyncManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    init {
        // Enable offline persistence
        db.firestoreSettings = com.google.firebase.firestore.firestoreSettings {
            isPersistenceEnabled = true
        }
    }

    /**
     * Check if device is online.
     */
    fun isOnline(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Sync all data to cloud.
     */
    suspend fun syncAllData(
        profile: AppProfile,
        customers: List<Customer>,
        transactions: List<TxnEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!authManager.isSignedIn()) {
            return@withContext Result.failure(Exception("User not signed in"))
        }

        if (!isOnline()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        _syncState.value = SyncState.Syncing

        try {
            val userId = authManager.getCurrentUserId()!!

            // Sync profile
            syncProfile(userId, profile)

            // Sync customers
            syncCustomers(userId, customers)

            // Sync transactions
            syncTransactions(userId, transactions)

            _lastSyncTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.Success

            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }

    /**
     * Download data from cloud.
     */
    suspend fun downloadFromCloud(): Result<CloudData> = withContext(Dispatchers.IO) {
        if (!authManager.isSignedIn()) {
            return@withContext Result.failure(Exception("User not signed in"))
        }

        if (!isOnline()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        _syncState.value = SyncState.Syncing

        try {
            val userId = authManager.getCurrentUserId()!!

            // Get profile
            val profileDoc = db.collection("users")
                .document(userId)
                .collection("profile")
                .document("main")
                .get()
                .await()

            val profile = if (profileDoc.exists()) {
                AppProfile(
                    businessName = profileDoc.getString("businessName") ?: "",
                    ownerName = profileDoc.getString("ownerName") ?: "",
                    phone = profileDoc.getString("phone") ?: "",
                    address = profileDoc.getString("address") ?: "",
                    gstNumber = profileDoc.getString("gstNumber") ?: "",
                    language = profileDoc.getString("language") ?: "en",
                    syncEnabled = true
                )
            } else null

            // Get customers
            val customersSnapshot = db.collection("users")
                .document(userId)
                .collection("customers")
                .get()
                .await()

            val customers = customersSnapshot.documents.mapNotNull { doc ->
                Customer(
                    id = doc.getLong("id") ?: 0L,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    phone = doc.getString("phone") ?: "",
                    address = doc.getString("address") ?: "",
                    photoPath = doc.getString("photoPath"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }

            // Get transactions
            val transactionsSnapshot = db.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .await()

            val transactions = transactionsSnapshot.documents.mapNotNull { doc ->
                TxnEntity(
                    id = doc.getLong("id") ?: 0L,
                    customerId = doc.getLong("customerId") ?: return@mapNotNull null,
                    type = com.nammasanthe.ledger.data.entity.TxnType.valueOf(
                        doc.getString("type") ?: "CREDIT"
                    ),
                    amount = doc.getDouble("amount") ?: 0.0,
                    note = doc.getString("note"),
                    date = doc.getLong("date") ?: System.currentTimeMillis(),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    syncPending = false,
                    photoPath = doc.getString("photoPath")
                )
            }

            _syncState.value = SyncState.Success

            Result.success(CloudData(profile, customers, transactions))
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    private suspend fun syncProfile(userId: String, profile: AppProfile) {
        val profileData = hashMapOf(
            "businessName" to profile.businessName,
            "ownerName" to profile.ownerName,
            "phone" to profile.phone,
            "address" to profile.address,
            "gstNumber" to profile.gstNumber,
            "language" to profile.language,
            "lastUpdated" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("profile")
            .document("main")
            .set(profileData)
            .await()
    }

    private suspend fun syncCustomers(userId: String, customers: List<Customer>) {
        val batch = db.batch()
        val customersRef = db.collection("users")
            .document(userId)
            .collection("customers")

        customers.forEach { customer ->
            val data = hashMapOf(
                "id" to customer.id,
                "name" to customer.name,
                "phone" to customer.phone,
                "address" to customer.address,
                "photoPath" to (customer.photoPath ?: ""),
                "createdAt" to customer.createdAt,
                "lastUpdated" to System.currentTimeMillis()
            )
            batch.set(customersRef.document(customer.id.toString()), data, SetOptions.merge())
        }

        batch.commit().await()
    }

    private suspend fun syncTransactions(userId: String, transactions: List<TxnEntity>) {
        val batch = db.batch()
        val transactionsRef = db.collection("users")
            .document(userId)
            .collection("transactions")

        transactions.forEach { txn ->
            val data = hashMapOf(
                "id" to txn.id,
                "customerId" to txn.customerId,
                "type" to txn.type.name,
                "amount" to txn.amount,
                "note" to (txn.note ?: ""),
                "date" to txn.date,
                "createdAt" to txn.createdAt,
                "photoPath" to (txn.photoPath ?: ""),
                "lastUpdated" to System.currentTimeMillis()
            )
            batch.set(transactionsRef.document(txn.id.toString()), data, SetOptions.merge())
        }

        batch.commit().await()
    }

    /**
     * Get sync status text.
     */
    fun getSyncStatusText(): String {
        return when (val state = _syncState.value) {
            is SyncState.Idle -> {
                val lastSync = _lastSyncTime.value
                if (lastSync != null) {
                    val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                    "Last synced: ${dateFormat.format(Date(lastSync))}"
                } else "Not synced yet"
            }
            is SyncState.Syncing -> "Syncing..."
            is SyncState.Success -> "Sync completed"
            is SyncState.Error -> "Sync failed: ${state.message}"
        }
    }
}

/**
 * Sync states.
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Data from cloud.
 */
data class CloudData(
    val profile: AppProfile?,
    val customers: List<Customer>,
    val transactions: List<TxnEntity>
)
