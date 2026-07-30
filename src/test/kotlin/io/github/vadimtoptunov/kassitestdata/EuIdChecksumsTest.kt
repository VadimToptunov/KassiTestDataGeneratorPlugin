package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.algo.EuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.LeiGenerator
import io.github.vadimtoptunov.kassitestdata.generators.NationalIdGenerator
import io.github.vadimtoptunov.kassitestdata.generators.TaxIdGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

class EuIdChecksumsTest {

    // ---- Reference vectors (anchor algorithms to published values) ----

    @Test
    fun `Spain DNI and NIE reference letters`() {
        assertTrue(EuIdChecksums.isValidSpanishDni("12345678Z"))   // 12345678 % 23 = 14 -> Z
        assertFalse(EuIdChecksums.isValidSpanishDni("12345678A"))
        assertTrue(EuIdChecksums.isValidSpanishDni("X1234567L"))   // NIE, X->0
        assertFalse(EuIdChecksums.isValidSpanishDni("X1234567Z"))
    }

    @Test
    fun `Portugal NIF mod-11 reference`() {
        assertTrue(EuIdChecksums.isValidPortugueseNif("123456789"))
        assertFalse(EuIdChecksums.isValidPortugueseNif("123456781"))
    }

    @Test
    fun `Poland PESEL reference (Wikipedia sample)`() {
        assertTrue(EuIdChecksums.isValidPesel("44051401458"))
        assertFalse(EuIdChecksums.isValidPesel("44051401459"))
    }

    @Test
    fun `Poland NIP reference`() {
        assertTrue(EuIdChecksums.isValidNip("1234567802"))
        assertFalse(EuIdChecksums.isValidNip("1234567803"))
    }

    @Test
    fun `Belgium VAT mod-97 reference`() {
        assertTrue(EuIdChecksums.isValidBelgianVat("0400000086"))
        assertFalse(EuIdChecksums.isValidBelgianVat("0400000087"))
    }

    /** Independent LEI mod-97 check via BigInteger (different implementation than production). */
    private fun referenceLeiValid(lei: String): Boolean {
        if (!Regex("^[A-Z0-9]{18}[0-9]{2}$").matches(lei)) return false
        val numeric = buildString {
            for (c in lei) if (c in '0'..'9') append(c) else append((c - 'A' + 10).toString())
        }
        return BigInteger(numeric).mod(BigInteger.valueOf(97)) == BigInteger.ONE
    }

    @Test
    fun `LEI ISO 17442 reference and generated values pass an independent mod-97`() {
        assertTrue(EuIdChecksums.isValidLei("5493001KJTIIGC8Y1R12")) // real published LEI
        val rng = Rng(2024L)
        repeat(50) {
            val lei = LeiGenerator.generate(rng, valid = true)
            assertTrue(referenceLeiValid(lei), "generated LEI must pass independent mod-97: $lei")
            assertFalse(referenceLeiValid(LeiGenerator.generate(rng, valid = false)))
        }
    }

    // ---- Generator round-trips: valid passes, invalid fails ----

    @Test
    fun `new national ID schemes round-trip`() {
        val rng = Rng(7L)
        for (country in listOf(Country.ES, Country.PT, Country.PL)) {
            repeat(50) {
                assertTrue(NationalIdGenerator.isValid(country, NationalIdGenerator.generate(country, rng, valid = true)))
                assertFalse(NationalIdGenerator.isValid(country, NationalIdGenerator.generate(country, rng, valid = false)))
            }
        }
    }

    @Test
    fun `new VAT schemes round-trip`() {
        val rng = Rng(11L)
        repeat(50) {
            assertTrue(EuIdChecksums.isValidNip(TaxIdGenerator.polishNip(rng, valid = true)))
            assertFalse(EuIdChecksums.isValidNip(TaxIdGenerator.polishNip(rng, valid = false)))
            assertTrue(Checksums.isLuhnValid(TaxIdGenerator.italianVat(rng, valid = true)))
            assertFalse(Checksums.isLuhnValid(TaxIdGenerator.italianVat(rng, valid = false)))
            assertTrue(EuIdChecksums.isValidBelgianVat(TaxIdGenerator.belgianVat(rng, valid = true)))
            assertFalse(EuIdChecksums.isValidBelgianVat(TaxIdGenerator.belgianVat(rng, valid = false)))
            assertTrue(EuIdChecksums.isValidPortugueseNif(TaxIdGenerator.portugueseVat(rng, valid = true)))
            assertFalse(EuIdChecksums.isValidPortugueseNif(TaxIdGenerator.portugueseVat(rng, valid = false)))
        }
    }

    @Test
    fun `PESEL encodes the persona date of birth`() {
        val p = io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator.generate(Country.PL, seed = 123L)
        val pesel = p.nationalId!!
        assertTrue(NationalIdGenerator.isValid(Country.PL, pesel))
        val yy = p.dateOfBirth.year % 100
        assertTrue(pesel.startsWith(yy.toString().padStart(2, '0')), "PESEL $pesel should start with DOB year $yy")
    }
}
