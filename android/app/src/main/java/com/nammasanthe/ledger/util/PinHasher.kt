package com.nammasanthe.ledger.util

import java.security.MessageDigest

object PinHasher {
    private const val SALT = "namma-santhe-v1"
    fun hash(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((SALT + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(pin: String, expected: String?): Boolean {
        if (expected.isNullOrEmpty()) return true
        return hash(pin) == expected
    }
}
