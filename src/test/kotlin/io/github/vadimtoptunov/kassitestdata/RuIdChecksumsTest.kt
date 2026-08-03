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
    fun `bank account keying reference (Sberbank correspondent account)`() {
        // БИК 044525225 + корсчёт 30101810400000000225 — weighted sum 110, 110 % 10 == 0
        assertTrue(RuIdChecksums.isValidRussianAccount("044525225", "30101810400000000225"))
        assertFalse(RuIdChecksums.isValidRussianAccount("044525225", "30101810400000000226"))
    }

    @Test
    fun `generated account keys correctly to its БИК`() {
        val rng = Rng(77L)
        repeat(100) {
            val bik = RussianIdGenerator.bik(rng)
            val account = RussianIdGenerator.account(rng, bik)
            assertTrue(bik.length == 9 && bik.startsWith("04"), bik)
            assertTrue(RuIdChecksums.isValidRussianAccount(bik, account), "$bik / $account")
            // corrupt the position-9 control key -> no longer keys to the same БИК
            val badKey = ((account[8] - '0') + 1) % 10
            val corrupted = account.substring(0, 8) + badKey + account.substring(9)
            assertFalse(RuIdChecksums.isValidRussianAccount(bik, corrupted), corrupted)
        }
    }

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
            // bank line: "БИК <9> · Счёт <20>" — the account must key to the БИК
            val m = Regex("БИК (\\d{9}) · Счёт (\\d{20})").find(p.bankValue)!!
            assertTrue(RuIdChecksums.isValidRussianAccount(m.groupValues[1], m.groupValues[2]), p.bankValue)
        }
    }

    @Test
    fun `Russia is wired into the national ID and tax catalogs`() {
        assertTrue(NationalIdGenerator.supported.containsKey(Country.RU))
        assertTrue(TaxIdGenerator.supported.containsKey(Country.RU))
    }
}
