package com.nammasanthe.ledger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nammasanthe.ledger.data.dao.ConfirmationDao
import com.nammasanthe.ledger.data.dao.CustomerDao
import com.nammasanthe.ledger.data.dao.ScanDao
import com.nammasanthe.ledger.data.dao.TransactionDao
import com.nammasanthe.ledger.data.dao.UsedNonceDao
import com.nammasanthe.ledger.data.entity.ConfirmationEntity
import com.nammasanthe.ledger.data.entity.Customer
import com.nammasanthe.ledger.data.entity.ScanData
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.data.entity.UsedNonce

@Database(
    entities = [
        Customer::class,
        TxnEntity::class,
        ScanData::class,
        ConfirmationEntity::class,
        UsedNonce::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao()     : CustomerDao
    abstract fun transactionDao()  : TransactionDao
    abstract fun scanDao()         : ScanDao
    abstract fun confirmationDao() : ConfirmationDao
    abstract fun usedNonceDao()    : UsedNonceDao

    companion object {

        /** Migration 2 → 3: add QR confirmation tables. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // confirmations table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS confirmations (
                        txnId           INTEGER NOT NULL PRIMARY KEY,
                        confirmedAt     INTEGER NOT NULL,
                        scannerDeviceId TEXT    NOT NULL,
                        vendorDeviceId  TEXT    NOT NULL,
                        trustLevel      TEXT    NOT NULL,
                        nonce           TEXT    NOT NULL,
                        FOREIGN KEY(txnId) REFERENCES transactions(id) ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_confirmations_txnId ON confirmations(txnId)"
                )

                // used_nonces table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS used_nonces (
                        nonce  TEXT    NOT NULL PRIMARY KEY,
                        txnId  INTEGER NOT NULL,
                        usedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /** Migration 3 → 4: add photoPath for transaction photo proof. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN photoPath TEXT")
            }
        }

        fun build(context: Context): AppDatabase = Room
            .databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "namma_santhe.db"
            )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }
}
