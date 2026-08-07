package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.CryptoChecksums
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.CryptoAddressGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CryptoChecksumsTest {

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    @Test
    fun `Keccak-256 known-answer vectors`() {
        assertEquals(
            "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470",
            hex(CryptoChecksums.keccak256(ByteArray(0))),
        )
        assertEquals(
            "4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45",
            hex(CryptoChecksums.keccak256("abc".toByteArray(Charsets.US_ASCII))),
        )
    }

    @Test
    fun `Bitcoin Base58Check reference (genesis address)`() {
        assertTrue(CryptoChecksums.isValidBase58Check("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))
        assertFalse(CryptoChecksums.isValidBase58Check("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb")) // last char changed
        assertFalse(CryptoChecksums.isValidBase58Check("not base58 0OIl"))
    }

    @Test
    fun `Ethereum EIP-55 reference addresses`() {
        for (a in listOf(
            "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
            "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
            "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
            "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
        )) {
            assertTrue(CryptoChecksums.isValidEip55(a), "expected valid EIP-55: $a")
        }
        // The same address all-lowercase is not a valid EIP-55 checksum.
        assertFalse(CryptoChecksums.isValidEip55("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed"))
    }

    @Test
    fun `generators round-trip - valid passes, invalid fails`() {
        val rng = Rng(2024L)
        repeat(100) {
            assertTrue(CryptoChecksums.isValidBase58Check(CryptoAddressGenerator.btcAddress(rng, valid = true)))
            assertFalse(CryptoChecksums.isValidBase58Check(CryptoAddressGenerator.btcAddress(rng, valid = false)))
            assertTrue(CryptoChecksums.isValidEip55(CryptoAddressGenerator.ethAddress(rng, valid = true)))
            assertFalse(CryptoChecksums.isValidEip55(CryptoAddressGenerator.ethAddress(rng, valid = false)))
        }
    }
}
