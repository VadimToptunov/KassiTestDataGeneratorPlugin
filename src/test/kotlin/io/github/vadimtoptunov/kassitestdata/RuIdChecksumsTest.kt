package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.RuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.NationalIdGenerator
import io.github.vadimtoptunov.kassitestdata.generators.RussianIdGenerator
import io.github.vadimtoptunov.kassitestdata.generators.TaxIdGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuIdChecksumsTest {

    // ---- Reference vectors (anchor to published values) ----

    @Test
    fun `INN legal reference`() {
        assertTrue(RuIdChecksums.isValidInnLegal("7830002293"))
        assertFalse(RuIdChecksums.isValidInnLegal("7830002294"))
    }

    @Test
    fun `INN individual reference`() {
        assertTrue(RuIdChecksums.isValidInnIndividual("500100732259"))
        assertFalse(RuIdChecksums.isValidInnIndividual("500100732258"))
    }

    @Test
    fun `SNILS reference`() {
        assertTrue(RuIdChecksums.isValidSnils("11223344595"))
        assertFalse(RuIdChecksums.isValidSnils("11223344596"))
    }

    @Test
    fun `OGRN reference (Sberbank)`() {
        assertTrue(RuIdChecksums.isValidOgrn("1027700132195"))
        assertFalse(RuIdChecksums.isValidOgrn("1027700132196"))
    }

    // ---- Generator round-trips: valid passes, invalid fails ----

    @Test
    fun `all Russian generators round-trip`() {
        val rng = Rng(2024L)
        repeat(100) {
            assertTrue(RuIdChecksums.isValidSnils(RussianIdGenerator.snils(rng, valid = true)))
            assertFalse(RuIdChecksums.isValidSnils(RussianIdGenerator.snils(rng, valid = false)))
            assertTrue(RuIdChecksums.isValidInnIndividual(RussianIdGenerator.innIndividual(rng, valid = true)))
            assertFalse(RuIdChecksums.isValidInnIndividual(RussianIdGenerator.innIndividual(rng, valid = false)))
            assertTrue(RuIdChecksums.isValidInnLegal(RussianIdGenerator.innLegal(rng, valid = true)))
            assertFalse(RuIdChecksums.isValidInnLegal(RussianIdGenerator.innLegal(rng, valid = false)))
            assertTrue(RuIdChecksums.isValidOgrn(RussianIdGenerator.ogrn(rng, valid = true)))
            assertFalse(RuIdChecksums.isValidOgrn(RussianIdGenerator.ogrn(rng, valid = false)))
            assertTrue(RuIdChecksums.isValidOgrnip(RussianIdGenerator.ogrnip(rng, valid = true)))
            assertFalse(RuIdChecksums.isValidOgrnip(RussianIdGenerator.ogrnip(rng, valid = false)))
        }
    }

    @Test
    fun `Russian persona carries a valid SNILS and INN`() {
        for (seed in 1L..8L) {
            val p = PersonaGenerator.generate(Country.RU, seed)
            assertTrue(NationalIdGenerator.isValid(Country.RU, p.nationalId!!), "СНИЛС: ${p.nationalId}")
            assertTrue(RuIdChecksums.isValidInnIndividual(p.taxId!!), "ИНН: ${p.taxId}")
        }
    }

    @Test
    fun `Russia is wired into the national ID and tax catalogs`() {
        assertTrue(NationalIdGenerator.supported.containsKey(Country.RU))
        assertTrue(TaxIdGenerator.supported.containsKey(Country.RU))
    }
}
