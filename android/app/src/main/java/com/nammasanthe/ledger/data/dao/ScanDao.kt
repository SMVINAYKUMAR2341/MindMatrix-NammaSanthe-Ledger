package com.nammasanthe.ledger.data.dao

import androidx.room.*
import com.nammasanthe.ledger.data.entity.ScanData
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScanData>>

    @Insert
    suspend fun insert(scan: ScanData): Long

    @Update
    suspend fun update(scan: ScanData)

    @Delete
    suspend fun delete(scan: ScanData)
}
