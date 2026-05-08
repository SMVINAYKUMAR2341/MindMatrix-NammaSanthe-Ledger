package com.nammasanthe.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammasanthe.ledger.data.entity.Customer
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.data.entity.TxnType
import com.nammasanthe.ledger.data.repo.LedgerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(private val repo: LedgerRepository) : ViewModel() {
    val customers = repo.customers().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val balances = repo.balances().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val outstanding = repo.outstanding().stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)
    val recent = repo.recentTransactions(20).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun customerTransactions(id: Long) = repo.customerTransactions(id)

    fun addCustomer(name: String, phone: String, address: String = "", photoPath: String? = null) = viewModelScope.launch {
        repo.addCustomer(Customer(name = name, phone = phone, address = address, photoPath = photoPath))
    }

    fun transactionsSince(since: Long) = repo.transactionsSince(since)

    fun addTransaction(customerId: Long, type: TxnType, amount: Double, note: String? = null) =
        viewModelScope.launch {
            repo.addTransaction(
                TxnEntity(customerId = customerId, type = type, amount = amount, note = note)
            )
        }

    suspend fun addCredit(cId: Long, amount: Double, note: String?, photoPath: String? = null): Long {
        val txn = TxnEntity(customerId = cId, type = TxnType.CREDIT, amount = amount, note = note, photoPath = photoPath)
        return repo.addTransaction(txn)
    }

    suspend fun addPayment(cId: Long, amount: Double, note: String?, photoPath: String? = null): Long {
        val txn = TxnEntity(customerId = cId, type = TxnType.PAYMENT, amount = amount, note = note, photoPath = photoPath)
        return repo.addTransaction(txn)
    }

    suspend fun getTransactionById(txnId: Long): TxnEntity? {
        return repo.getTransactionById(txnId)
    }

    suspend fun updateTransactionPhoto(transactionId: Long, photoPath: String) {
        repo.updateTransactionPhoto(transactionId, photoPath)
    }

    /** Add a transaction and return the new row ID (for QR generation). */
    suspend fun addTransactionGetId(
        customerId : Long,
        type       : TxnType,
        amount     : Double,
        note       : String? = null
    ): Long = repo.addTransaction(
        TxnEntity(customerId = customerId, type = type, amount = amount, note = note)
    )

    suspend fun getCustomer(id: Long) = repo.getCustomer(id)

    suspend fun getAllTransactions(): List<TxnEntity> {
        return repo.getAllTransactions()
    }

    fun deleteCustomer(c: Customer) = viewModelScope.launch { repo.deleteCustomer(c) }
}
