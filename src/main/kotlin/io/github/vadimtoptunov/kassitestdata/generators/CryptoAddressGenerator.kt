package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.CryptoChecksums
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * Cryptocurrency addresses with real checksums: a Bitcoin legacy P2PKH address (Base58Check over a
 * random 20-byte hash160) and an Ethereum address (random 20 bytes with the EIP-55 case checksum).
 * The invalid variants deliberately break the checksum.
 */
object CryptoAddressGenerator {

    /** Bitcoin legacy P2PKH mainnet address (version byte 0x00). */
    fun btcAddress(rng: Rng, valid: Boolean): String {
        val payload = ByteArray(21)
        payload[0] = 0x00 // mainnet P2PKH version
        for (i in 1 until 21) payload[i] = rng.int(256).toByte()
        if (valid) return CryptoChecksums.base58CheckEncode(payload)
        // Corrupt the checksum directly so the string fails Base58Check verification.
        val badChecksum = CryptoChecksums.doubleSha256(payload).copyOf(4)
        badChecksum[0] = (badChecksum[0] + 1).toByte()
        return CryptoChecksums.base58Encode(payload + badChecksum)
    }

    /** Ethereum address with the EIP-55 mixed-case checksum. */
    fun ethAddress(rng: Rng, valid: Boolean): String {
        while (true) {
            val bytes = ByteArray(20) { rng.int(256).toByte() }
            val hex = bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            val checksummed = CryptoChecksums.toEip55(hex)
            if (valid) return "0x$checksummed"
            // Break the checksum by flipping the case of the first letter (retry the rare all-digit case).
            val chars = checksummed.toCharArray()
            val idx = chars.indexOfFirst { it in 'a'..'f' || it in 'A'..'F' }
            if (idx < 0) continue
            chars[idx] = if (chars[idx].isUpperCase()) chars[idx].lowercaseChar() else chars[idx].uppercaseChar()
            return "0x${String(chars)}"
        }
    }
}
