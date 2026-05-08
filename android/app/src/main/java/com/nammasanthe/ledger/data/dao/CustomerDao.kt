package com.nammasanthe.ledger.data.dao

import androidx.room.*
import com.nammasanthe.ledger.data.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<Customer>>

    @Insert
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)
}
