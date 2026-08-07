package io.github.vadimtoptunov.kassitestdata.algo

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Cryptocurrency-address checksums, all real:
 *  - Bitcoin Base58Check (version byte + payload + first 4 bytes of double-SHA-256).
 *  - Ethereum EIP-55 mixed-case checksum, which needs genuine **Keccac-256** — note this is the
 *    original Keccak, NOT FIPS-202 SHA3-256 (they differ in the padding byte), so the JDK's SHA3
 *    can't be used and Keccak is implemented here from scratch and anchored to published vectors.
 */
object CryptoChecksums {

    // ------------------------------------------------------------------ SHA-256 / double SHA-256

    private fun sha256(b: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(b)

    fun doubleSha256(b: ByteArray): ByteArray = sha256(sha256(b))

    // ------------------------------------------------------------------ Base58 / Base58Check

    private const val B58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun base58Encode(data: ByteArray): String {
        var zeros = 0
        while (zeros < data.size && data[zeros].toInt() == 0) zeros++
        var num = BigInteger(1, data)
        val base = BigInteger.valueOf(58)
        val sb = StringBuilder()
        while (num > BigInteger.ZERO) {
            val qr = num.divideAndRemainder(base)
            sb.append(B58[qr[1].toInt()])
            num = qr[0]
        }
        repeat(zeros) { sb.append('1') }
        return sb.reverse().toString()
    }

    fun base58Decode(s: String): ByteArray? {
        val base = BigInteger.valueOf(58)
        var num = BigInteger.ZERO
        for (c in s) {
            val idx = B58.indexOf(c)
            if (idx < 0) return null
            num = num.multiply(base).add(BigInteger.valueOf(idx.toLong()))
        }
        var bytes = num.toByteArray()
        if (bytes.size > 1 && bytes[0].toInt() == 0) bytes = bytes.copyOfRange(1, bytes.size)
        var zeros = 0
        while (zeros < s.length && s[zeros] == '1') zeros++
        val result = ByteArray(zeros + bytes.size)
        System.arraycopy(bytes, 0, result, zeros, bytes.size)
        return result
    }

    /** Base58Check-encode a version+payload buffer: append the 4-byte double-SHA-256 checksum. */
    fun base58CheckEncode(versionAndPayload: ByteArray): String =
        base58Encode(versionAndPayload + doubleSha256(versionAndPayload).copyOf(4))

    /** Validate a Base58Check string: last 4 bytes must equal double-SHA-256(payload)[0..3]. */
    fun isValidBase58Check(s: String): Boolean {
        val decoded = base58Decode(s) ?: return false
        if (decoded.size < 5) return false
        val payload = decoded.copyOf(decoded.size - 4)
        val checksum = decoded.copyOfRange(decoded.size - 4, decoded.size)
        return doubleSha256(payload).copyOf(4).contentEquals(checksum)
    }

    // ------------------------------------------------------------------ Keccac-256 (Ethereum)

    private val KECCAK_RC = longArrayOf(
        0x0000000000000001uL.toLong(), 0x0000000000008082uL.toLong(), 0x800000000000808auL.toLong(),
        0x8000000080008000uL.toLong(), 0x000000000000808buL.toLong(), 0x0000000080000001uL.toLong(),
        0x8000000080008081uL.toLong(), 0x8000000000008009uL.toLong(), 0x000000000000008auL.toLong(),
        0x0000000000000088uL.toLong(), 0x0000000080008009uL.toLong(), 0x000000008000000auL.toLong(),
        0x000000008000808buL.toLong(), 0x800000000000008buL.toLong(), 0x8000000000008089uL.toLong(),
        0x8000000000008003uL.toLong(), 0x8000000000008002uL.toLong(), 0x8000000000000080uL.toLong(),
        0x000000000000800auL.toLong(), 0x800000008000000auL.toLong(), 0x8000000080008081uL.toLong(),
        0x8000000000008080uL.toLong(), 0x0000000080000001uL.toLong(), 0x8000000080008008uL.toLong(),
    )
    private val KECCAK_ROT = intArrayOf(
        0, 1, 62, 28, 27, 36, 44, 6, 55, 20, 3, 10, 43, 25, 39, 41, 45, 15, 21, 8, 18, 2, 61, 56, 14,
    )

    private fun rotl(x: Long, n: Int): Long = if (n == 0) x else (x shl n) or (x ushr (64 - n))

    private fun keccakF1600(a: LongArray) {
        for (round in 0 until 24) {
            val c = LongArray(5) { x -> a[x] xor a[x + 5] xor a[x + 10] xor a[x + 15] xor a[x + 20] }
            val d = LongArray(5) { x -> c[(x + 4) % 5] xor rotl(c[(x + 1) % 5], 1) }
            for (x in 0 until 5) for (y in 0 until 5) a[x + 5 * y] = a[x + 5 * y] xor d[x]
            val b = LongArray(25)
            for (x in 0 until 5) for (y in 0 until 5) {
                b[y + 5 * ((2 * x + 3 * y) % 5)] = rotl(a[x + 5 * y], KECCAK_ROT[x + 5 * y])
            }
            for (x in 0 until 5) for (y in 0 until 5) {
                a[x + 5 * y] = b[x + 5 * y] xor (b[(x + 1) % 5 + 5 * y].inv() and b[(x + 2) % 5 + 5 * y])
            }
            a[0] = a[0] xor KECCAK_RC[round]
        }
    }

    fun keccak256(input: ByteArray): ByteArray {
        val rate = 136 // bytes (1088-bit rate for Keccac-256)
        val padLen = rate - (input.size % rate)
        val padded = input.copyOf(input.size + padLen)
        padded[input.size] = (padded[input.size].toInt() or 0x01).toByte() // Keccak pad (not SHA3's 0x06)
        padded[padded.size - 1] = (padded[padded.size - 1].toInt() or 0x80).toByte()

        val state = LongArray(25)
        var offset = 0
        while (offset < padded.size) {
            for (i in 0 until rate / 8) state[i] = state[i] xor leLong(padded, offset + i * 8)
            keccakF1600(state)
            offset += rate
        }
        val out = ByteArray(32)
        for (i in 0 until 4) putLeLong(out, i * 8, state[i])
        return out
    }

    private fun leLong(b: ByteArray, off: Int): Long {
        var r = 0L
        for (i in 0 until 8) r = r or ((b[off + i].toLong() and 0xFF) shl (8 * i))
        return r
    }

    private fun putLeLong(b: ByteArray, off: Int, v: Long) {
        for (i in 0 until 8) b[off + i] = ((v ushr (8 * i)) and 0xFF).toByte()
    }

    // ------------------------------------------------------------------ Ethereum EIP-55

    /** Apply the EIP-55 mixed-case checksum to a 40-char lowercase hex address (no 0x prefix). */
    fun toEip55(hexNo0x: String): String {
        val lower = hexNo0x.lowercase()
        val hash = keccak256(lower.toByteArray(Charsets.US_ASCII))
        return buildString {
            for (i in lower.indices) {
                val ch = lower[i]
                if (ch in 'a'..'f') {
                    val nibble = (hash[i / 2].toInt() ushr (if (i % 2 == 0) 4 else 0)) and 0x0F
                    append(if (nibble >= 8) ch.uppercaseChar() else ch)
                } else {
                    append(ch)
                }
            }
        }
    }

    /** True only for a correctly EIP-55-checksummed address (0x + 40 hex, case matching the checksum). */
    fun isValidEip55(address: String): Boolean {
        if (!Regex("^0x[0-9a-fA-F]{40}$").matches(address)) return false
        val body = address.substring(2)
        return toEip55(body) == body
    }
}
