package com.nammasanthe.ledger.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nammasanthe.ledger.data.entity.ConfirmationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfirmationDao {

    /**
     * Insert a new confirmation.
     * IGNORE strategy: a txn can only be confirmed once (unique index on txnId).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(confirmation: ConfirmationEntity): Long

    @Query("SELECT * FROM confirmations WHERE txnId = :txnId LIMIT 1")
    suspend fun getByTxnId(txnId: Long): ConfirmationEntity?

    @Query("SELECT * FROM confirmations WHERE txnId = :txnId LIMIT 1")
    fun observeByTxnId(txnId: Long): Flow<ConfirmationEntity?>

    @Query("SELECT * FROM confirmations ORDER BY confirmedAt DESC")
    fun observeAll(): Flow<List<ConfirmationEntity>>

    // NO DELETE method — hard-delete of confirmations is forbidden by security rules.
}
