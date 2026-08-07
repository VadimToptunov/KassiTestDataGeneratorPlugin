package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.algo.EuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.IdentifierGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IdentifierGeneratorTest {

    private fun isValidEan13(v: String) =
        v.length == 13 && v.all { it in '0'..'9' } && EuIdChecksums.ean13Check(v.substring(0, 12)) == (v[12] - '0')

    @Test
    fun `ISIN reference values (real securities)`() {
        assertTrue(Checksums.isValidIsin("US0378331005")) // Apple
        assertTrue(Checksums.isValidIsin("US5949181045")) // Microsoft
        assertTrue(Checksums.isValidIsin("GB0002634946")) // BAE Systems
        assertTrue(Checksums.isValidIsin("AU0000XVGZA3")) // contains letters in the NSIN
        assertFalse(Checksums.isValidIsin("US0378331004"))
    }

    @Test
    fun `IMEI and EAN-13 reference values`() {
        assertTrue(Checksums.isLuhnValid("490154203237518")) // classic valid test IMEI
        assertFalse(Checksums.isLuhnValid("490154203237510"))
        assertTrue(isValidEan13("5901234123457")) // valid EAN-13
        assertFalse(isValidEan13("5901234123450"))
    }

    @Test
    fun `VIN reference values (real check characters)`() {
        assertTrue(Checksums.isValidVin("1M8GDM9AXKP042788")) // canonical NHTSA example, check char 'X'
        assertTrue(Checksums.isValidVin("11111111111111111")) // all ones → check digit 1
        assertFalse(Checksums.isValidVin("1M8GDM9A0KP042788")) // check char wrong (0 instead of X)
        assertFalse(Checksums.isValidVin("1M8GDM9AXKP04278I")) // contains illegal 'I'
    }

    @Test
    fun `ISBN-10 and ISBN-13 reference values`() {
        assertTrue(Checksums.isValidIsbn10("0306406152")) // 0-306-40615-2
        assertTrue(Checksums.isValidIsbn10("097522980X")) // 0-9752298-0-X (check char X)
        assertTrue(Checksums.isValidIsbn10("0-306-40615-2")) // hyphens ignored
        assertFalse(Checksums.isValidIsbn10("0306406153"))
        assertTrue(isValidEan13("9780306406157")) // ISBN-13 978-0-306-40615-7
        assertFalse(isValidEan13("9780306406158"))
    }

    @Test
    fun `generators round-trip - valid passes, invalid fails`() {
        val rng = Rng(2024L)
        repeat(100) {
            assertTrue(Checksums.isValidIsin(IdentifierGenerator.isin(rng, valid = true)))
            assertFalse(Checksums.isValidIsin(IdentifierGenerator.isin(rng, valid = false)))
            assertTrue(Checksums.isLuhnValid(IdentifierGenerator.imei(rng, valid = true)))
            assertFalse(Checksums.isLuhnValid(IdentifierGenerator.imei(rng, valid = false)))
            assertTrue(isValidEan13(IdentifierGenerator.ean13(rng, valid = true)))
            assertFalse(isValidEan13(IdentifierGenerator.ean13(rng, valid = false)))
            assertTrue(Checksums.isValidVin(IdentifierGenerator.vin(rng, valid = true)))
            assertFalse(Checksums.isValidVin(IdentifierGenerator.vin(rng, valid = false)))
            assertTrue(Checksums.isValidIsbn10(IdentifierGenerator.isbn10(rng, valid = true)))
            assertFalse(Checksums.isValidIsbn10(IdentifierGenerator.isbn10(rng, valid = false)))
            assertTrue(isValidEan13(IdentifierGenerator.isbn13(rng, valid = true)))
            assertFalse(isValidEan13(IdentifierGenerator.isbn13(rng, valid = false)))
        }
    }
}
