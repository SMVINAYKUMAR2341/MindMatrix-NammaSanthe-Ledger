package com.nammasanthe.ledger.security

enum class TrustLevel {
    /** Different device, valid nonce, within expiry, scan delay ok. */
    VERIFIED,
    /** Same device as vendor, too-fast scan, or any anomaly detected. */
    SUSPICIOUS,
    /** No confirmation scan has been performed yet. */
    UNVERIFIED
}
