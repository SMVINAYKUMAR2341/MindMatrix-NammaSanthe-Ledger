package com.nammasanthe.ledger.data.db

import androidx.room.TypeConverter
import com.nammasanthe.ledger.data.entity.TxnType
import com.nammasanthe.ledger.security.TrustLevel

class Converters {
    // ── TxnType ──────────────────────────────────────────────────────────────
    @TypeConverter fun fromTxnType(t: TxnType)   : String  = t.name
    @TypeConverter fun toTxnType  (s: String)    : TxnType = TxnType.valueOf(s)

    // ── TrustLevel ───────────────────────────────────────────────────────────
    @TypeConverter fun fromTrustLevel(t: TrustLevel): String     = t.name
    @TypeConverter fun toTrustLevel  (s: String)    : TrustLevel = TrustLevel.valueOf(s)
}
