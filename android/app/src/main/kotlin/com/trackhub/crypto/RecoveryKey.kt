package com.trackhub.crypto

/** Recovery key formatting/parsing. Matches client: 8 groups of 8 hex chars, space-separated. */
object RecoveryKey {

    fun format(key: ByteArray): String {
        val hex = Crypto.toHex(key)
        return (0 until hex.length step 8).joinToString(" ") { hex.substring(it, it + 8) }
    }

    fun parse(input: String): ByteArray {
        val clean = input.replace(Regex("\\s+"), "").lowercase()
        require(clean.matches(Regex("[0-9a-f]{64}"))) { "Recovery key must be 64 hex characters" }
        return Crypto.fromHex(clean)
    }
}
