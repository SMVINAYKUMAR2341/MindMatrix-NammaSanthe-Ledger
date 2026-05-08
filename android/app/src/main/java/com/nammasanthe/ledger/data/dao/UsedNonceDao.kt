package com.nammasanthe.ledger.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nammasanthe.ledger.data.entity.UsedNonce

@Dao
interface UsedNonceDao {

    /**
     * Attempt to claim a nonce.
     * Returns the new rowid on success, or -1 if the nonce already exists
     * (IGNORE conflict strategy + UNIQUE primary key).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun claim(nonce: UsedNonce): Long

    @Query("SELECT COUNT(*) FROM used_nonces WHERE nonce = :nonce")
    suspend fun count(nonce: String): Int

    /** Returns true if this nonce has already been used. */
    suspend fun isUsed(nonce: String): Boolean = count(nonce) > 0

    // NO DELETE method — nonce log must be permanent.
}
