package com.nammasanthe.ledger

import android.app.Application
import com.nammasanthe.ledger.data.db.AppDatabase
import com.nammasanthe.ledger.data.repo.ConfirmationRepository
import com.nammasanthe.ledger.data.repo.LedgerRepository
import com.nammasanthe.ledger.data.repo.ProfileStore

class NammaSantheApp : Application() {
    lateinit var db                  : AppDatabase           private set
    lateinit var repository          : LedgerRepository      private set
    lateinit var profileStore        : ProfileStore          private set
    lateinit var confirmationRepo    : ConfirmationRepository private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = AppDatabase.build(this)
        repository       = LedgerRepository(db.customerDao(), db.transactionDao(), db.scanDao())
        confirmationRepo = ConfirmationRepository(db.confirmationDao(), db.usedNonceDao())
        profileStore     = ProfileStore(this)
    }

    companion object {
        lateinit var instance: NammaSantheApp
            private set
    }
}
