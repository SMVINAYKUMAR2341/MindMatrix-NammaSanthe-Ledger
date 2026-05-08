package com.nammasanthe.ledger.data.dao

import androidx.room.*
import com.nammasanthe.ledger.data.entity.TxnEntity
import kotlinx.coroutines.flow.Flow

data class CustomerBalance(
    val customerId: Long,
    val name: String,
    val phone: String,
    val photoPath: String?,
    val totalCredit: Double,
    val totalPayment: Double,
    val balance: Double,
    val lastTxn: Long?
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY date DESC")
    fun observeByCustomer(customerId: Long): Flow<List<TxnEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<TxnEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :since ORDER BY date ASC")
    fun observeSince(since: Long): Flow<List<TxnEntity>>

    @Query("SELECT * FROM transactions WHERE syncPending = 1")
    suspend fun pendingSync(): List<TxnEntity>

    @Query("""
        SELECT IFNULL(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0)
        FROM transactions
    """)
    fun observeOutstanding(): Flow<Double>

    @Query("""
        SELECT c.id AS customerId, c.name AS name, c.phone AS phone, c.photoPath AS photoPath,
            IFNULL(SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE 0 END), 0) AS totalCredit,
            IFNULL(SUM(CASE WHEN t.type = 'PAYMENT' THEN t.amount ELSE 0 END), 0) AS totalPayment,
            IFNULL(SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE -t.amount END), 0) AS balance,
            MAX(t.date) AS lastTxn
        FROM customers c
        LEFT JOIN transactions t ON t.customerId = c.id
        GROUP BY c.id
        ORDER BY balance DESC
    """)
    fun observeBalances(): Flow<List<CustomerBalance>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TxnEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<TxnEntity>

    @Insert
    suspend fun insert(txn: TxnEntity): Long

    @Update
    suspend fun update(txn: TxnEntity)

    @Delete
    suspend fun delete(txn: TxnEntity)
}
