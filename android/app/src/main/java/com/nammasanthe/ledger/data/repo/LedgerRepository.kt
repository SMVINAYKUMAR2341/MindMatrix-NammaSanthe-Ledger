package com.nammasanthe.ledger.data.repo

import com.nammasanthe.ledger.data.dao.CustomerDao
import com.nammasanthe.ledger.data.dao.ScanDao
import com.nammasanthe.ledger.data.dao.TransactionDao
import com.nammasanthe.ledger.data.entity.Customer
import com.nammasanthe.ledger.data.entity.ScanData
import com.nammasanthe.ledger.data.entity.TxnEntity

class LedgerRepository(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val scanDao: ScanDao
) {
    fun customers() = customerDao.observeAll()
    fun balances() = transactionDao.observeBalances()
    fun outstanding() = transactionDao.observeOutstanding()
    fun recentTransactions(limit: Int = 20) = transactionDao.observeRecent(limit)
    fun transactionsSince(since: Long) = transactionDao.observeSince(since)
    fun customerTransactions(id: Long) = transactionDao.observeByCustomer(id)
    fun searchCustomers(q: String) = customerDao.search(q)
    fun scans() = scanDao.observeAll()

    suspend fun getCustomer(id: Long) = customerDao.getById(id)
    suspend fun getTransactionById(id: Long) = transactionDao.getById(id)
    suspend fun addCustomer(c: Customer) = customerDao.insert(c)
    suspend fun updateCustomer(c: Customer) = customerDao.update(c)
    suspend fun deleteCustomer(c: Customer) = customerDao.delete(c)

    suspend fun addTransaction(t: TxnEntity) = transactionDao.insert(t)
    suspend fun updateTransaction(t: TxnEntity) = transactionDao.update(t)
    suspend fun deleteTransaction(t: TxnEntity) = transactionDao.delete(t)
    suspend fun pendingSyncTransactions() = transactionDao.pendingSync()

    /**
     * Update only the photo path for a transaction.
     */
    suspend fun updateTransactionPhoto(transactionId: Long, photoPath: String) {
        val txn = transactionDao.getById(transactionId) ?: return
        transactionDao.update(txn.copy(photoPath = photoPath))
    }

    /**
     * Get all transactions for export.
     */
    suspend fun getAllTransactions(): List<TxnEntity> {
        return transactionDao.getAll()
    }

    suspend fun saveScan(scan: ScanData) = scanDao.insert(scan)
}
