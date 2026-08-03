package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.algo.EuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.LeiGenerator
import io.github.vadimtoptunov.kassitestdata.generators.NationalIdGenerator
import io.github.vadimtoptunov.kassitestdata.generators.TaxIdGenerator
import org.junit.jupiter.api.Assertions.assertEquals
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
            assertTrue(EuIdChecksums.isValidFrenchVat(TaxIdGenerator.frenchVat(rng, valid = true)))
            assertFalse(EuIdChecksums.isValidFrenchVat(TaxIdGenerator.frenchVat(rng, valid = false)))
            assertTrue(EuIdChecksums.isValidSpanishCifTypeA(TaxIdGenerator.spanishCif(rng, valid = true)))
            assertFalse(EuIdChecksums.isValidSpanishCifTypeA(TaxIdGenerator.spanishCif(rng, valid = false)))
        }
    }

    @Test
    fun `France VAT key reference`() {
        assertTrue(EuIdChecksums.isValidFrenchVat("83404833048")) // key 83 over the Luhn-valid SIREN 404833048
        assertFalse(EuIdChecksums.isValidFrenchVat("84404833048"))
    }

    @Test
    fun `Spain CIF type A reference`() {
        assertTrue(EuIdChecksums.isValidSpanishCifTypeA("A58818501"))
        assertFalse(EuIdChecksums.isValidSpanishCifTypeA("A58818502"))
    }

    @Test
    fun `Italy Codice Fiscale control character reference`() {
        assertTrue(EuIdChecksums.isValidItalianCf("RSSMRA85T10A562S")) // Mario Rossi, the classic CF example
        assertFalse(EuIdChecksums.isValidItalianCf("RSSMRA85T10A562T"))
    }

    @Test
    fun `Italy Codice Fiscale encodes name, birth and sex`() {
        val rng = Rng(1L)
        val male = io.github.vadimtoptunov.kassitestdata.core.Gender.MALE
        val female = io.github.vadimtoptunov.kassitestdata.core.Gender.FEMALE
        val cf = NationalIdGenerator.generate(
            Country.IT, rng, valid = true,
            birth = java.time.LocalDate.of(1985, 12, 10), gender = male, surname = "Rossi", givenName = "Mario",
        )
        assertTrue(NationalIdGenerator.isValid(Country.IT, cf), cf)
        assertTrue(cf.startsWith("RSSMRA85T10"), cf) // RSS+MRA, year 85, month T (Dec), day 10
        val cfFemale = NationalIdGenerator.generate(
            Country.IT, rng, valid = true,
            birth = java.time.LocalDate.of(1985, 12, 10), gender = female, surname = "Rossi", givenName = "Maria",
        )
        assertTrue(cfFemale.startsWith("RSSMRA85T50"), cfFemale) // female: day 10 + 40 = 50
    }

    @Test
    fun `Italian persona Codice Fiscale is coherent with DOB and sex`() {
        for (seed in 1L..8L) {
            val p = io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator.generate(Country.IT, seed)
            val cf = p.nationalId!!
            assertTrue(NationalIdGenerator.isValid(Country.IT, cf), cf)
            assertEquals((p.dateOfBirth.year % 100).toString().padStart(2, '0'), cf.substring(6, 8), cf)
            assertEquals("ABCDEHLMPRST"[p.dateOfBirth.monthValue - 1], cf[8])
            val day = cf.substring(9, 11).toInt()
            val expectedDay = p.dateOfBirth.dayOfMonth +
                if (p.gender == io.github.vadimtoptunov.kassitestdata.core.Gender.FEMALE) 40 else 0
            assertEquals(expectedDay, day, cf)
        }
    }

    @Test
    fun `France NIR reference and generated values pass an independent mod-97`() {
        val rng = Rng(3L)
        repeat(50) {
            val nir = NationalIdGenerator.generate(Country.FR, rng, valid = true)
            assertTrue(EuIdChecksums.isValidFrenchNir(nir), "valid NIR: $nir")
            val independentKey = 97 - BigInteger(nir.substring(0, 13)).mod(BigInteger.valueOf(97)).toInt()
            assertTrue(nir.substring(13).toInt() == independentKey, "independent key mismatch: $nir")
            assertFalse(EuIdChecksums.isValidFrenchNir(NationalIdGenerator.generate(Country.FR, rng, valid = false)))
        }
    }

    @Test
    fun `Belgium national number mod-97 reference`() {
        assertTrue(EuIdChecksums.isValidBelgianNationalNumber("93051822361")) // 930518223 % 97 = 36 -> 61
        assertFalse(EuIdChecksums.isValidBelgianNationalNumber("93051822362"))
    }

    @Test
    fun `Sweden personnummer Luhn reference`() {
        assertTrue(EuIdChecksums.isValidSwedishPersonnummer("8112189876")) // documented valid personnummer
        assertFalse(EuIdChecksums.isValidSwedishPersonnummer("8112189875"))
    }

    @Test
    fun `FR BE SE personas encode the date of birth and sex`() {
        for ((country, length) in listOf(Country.FR to 15, Country.BE to 11, Country.SE to 10)) {
            for (seed in 1L..8L) { // a range of seeds to cover both sexes
                val persona = io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator.generate(country, seed)
                val id = persona.nationalId!!
                assertTrue(id.length == length, "$country id length: $id")
                assertTrue(NationalIdGenerator.isValid(country, id), "$country valid: $id")
                val yy = (persona.dateOfBirth.year % 100).toString().padStart(2, '0')
                val mm = persona.dateOfBirth.monthValue.toString().padStart(2, '0')
                val dd = persona.dateOfBirth.dayOfMonth.toString().padStart(2, '0')
                val male = persona.gender == io.github.vadimtoptunov.kassitestdata.core.Gender.MALE
                when (country) {
                    Country.FR -> {
                        assertTrue(id[0] == if (male) '1' else '2', "FR sex digit: $id")
                        assertTrue(id.substring(1, 3) == yy && id.substring(3, 5) == mm, "FR DOB: $id")
                    }
                    else -> { // BE and SE are YYMMDD-prefixed
                        assertTrue(id.startsWith("$yy$mm$dd"), "$country DOB: $id")
                        val sexDigit = if (country == Country.BE) id.substring(6, 9).toInt() else id[8] - '0'
                        assertTrue((sexDigit % 2 == 1) == male, "$country sex parity: $id")
                    }
                }
            }
        }
    }

    @Test
    fun `Finland HETU mod-31 reference`() {
        assertTrue(EuIdChecksums.isValidFinnishHetu("131052-308T")) // classic reference HETU
        assertFalse(EuIdChecksums.isValidFinnishHetu("131052-308U"))
    }

    @Test
    fun `Norway fodselsnummer mod-11 reference`() {
        assertTrue(EuIdChecksums.isValidNorwegianFnr("11111110060")) // k1=6, k2=0
        assertFalse(EuIdChecksums.isValidNorwegianFnr("11111110061"))
    }

    @Test
    fun `FI NO personas encode the date of birth and sex`() {
        for (country in listOf(Country.FI, Country.NO)) {
            for (seed in 1L..8L) {
                val persona = io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator.generate(country, seed)
                val id = persona.nationalId!!
                assertTrue(NationalIdGenerator.isValid(country, id), "$country valid: $id")
                val dd = persona.dateOfBirth.dayOfMonth.toString().padStart(2, '0')
                val mm = persona.dateOfBirth.monthValue.toString().padStart(2, '0')
                val yy = (persona.dateOfBirth.year % 100).toString().padStart(2, '0')
                assertTrue(id.startsWith("$dd$mm$yy"), "$country DDMMYY: $id") // both are day-first
                val male = persona.gender == io.github.vadimtoptunov.kassitestdata.core.Gender.MALE
                // FI: individual number is chars 7..9; NO: the 9th digit (index 8).
                val sexDigit = if (country == Country.FI) id.substring(7, 10).toInt() else id[8] - '0'
                assertTrue((sexDigit % 2 == 1) == male, "$country sex parity: $id")
            }
        }
    }

    @Test
    fun `Estonia isikukood two-round mod-11 reference`() {
        assertTrue(EuIdChecksums.isValidEstonianLithuanian("37605030299")) // documented Estonian code
        assertFalse(EuIdChecksums.isValidEstonianLithuanian("37605030298"))
    }

    @Test
    fun `Czech Slovak rodne cislo divisible-by-11 reference`() {
        assertTrue(EuIdChecksums.isValidCzechSlovakBirthNumber("7103192745")) // 7103192745 % 11 == 0
        assertFalse(EuIdChecksums.isValidCzechSlovakBirthNumber("7103192746"))
    }

    @Test
    fun `EE LT CZ SK personas encode DOB and sex`() {
        for (country in listOf(Country.EE, Country.LT, Country.CZ, Country.SK)) {
            for (seed in 1L..8L) {
                val p = io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator.generate(country, seed)
                val id = p.nationalId!!
                assertTrue(NationalIdGenerator.isValid(country, id), "$country: $id")
                val yy = (p.dateOfBirth.year % 100).toString().padStart(2, '0')
                val male = p.gender == io.github.vadimtoptunov.kassitestdata.core.Gender.MALE
                if (country == Country.EE || country == Country.LT) {
                    assertEquals(yy, id.substring(1, 3), id) // G YY MM DD ...
                    assertEquals(male, (id[0] - '0') % 2 == 1, id) // G odd = male
                } else { // CZ / SK: YY MM(+offset) DD ...
                    assertEquals(yy, id.substring(0, 2), id)
                    val rawMonth = id.substring(2, 4).toInt()
                    val female = rawMonth in 51..62 || rawMonth in 71..82
                    assertEquals(!male, female, id)
                }
            }
        }
    }

    @Test
    fun `Romania CNP weighted mod-11 reference`() {
        // Built per the published 279146358279 key: 1|80|05|15|10|001 -> weighted sum 113, 113 % 11 = 3.
        assertTrue(EuIdChecksums.isValidRomanianCnp("1800515100013"))
        assertFalse(EuIdChecksums.isValidRomanianCnp("1800515100014"))
    }

    @Test
    fun `Greece AMKA is Luhn-validated`() {
        val rng = Rng(9L)
        repeat(50) {
            assertTrue(NationalIdGenerator.isValid(Country.GR, NationalIdGenerator.generate(Country.GR, rng, valid = true)))
            assertFalse(NationalIdGenerator.isValid(Country.GR, NationalIdGenerator.generate(Country.GR, rng, valid = false)))
        }
    }

    @Test
    fun `RO and GR personas encode the date of birth`() {
        for (seed in 1L..8L) {
            val ro = io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator.generate(Country.RO, seed)
            val cnp = ro.nationalId!!
            assertTrue(NationalIdGenerator.isValid(Country.RO, cnp), cnp)
            assertEquals((ro.dateOfBirth.year % 100).toString().padStart(2, '0'), cnp.substring(1, 3), cnp)
            val roMale = ro.gender == io.github.vadimtoptunov.kassitestdata.core.Gender.MALE
            assertEquals(roMale, (cnp[0] - '0') % 2 == 1, cnp) // S odd = male

            val gr = io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator.generate(Country.GR, seed)
            val amka = gr.nationalId!!
            assertTrue(NationalIdGenerator.isValid(Country.GR, amka), amka)
            val ddmmyy = gr.dateOfBirth.dayOfMonth.toString().padStart(2, '0') +
                gr.dateOfBirth.monthValue.toString().padStart(2, '0') +
                (gr.dateOfBirth.year % 100).toString().padStart(2, '0')
            assertEquals(ddmmyy, amka.substring(0, 6), amka)
        }
    }

    @Test
    fun `Bulgaria EGN weighted mod-11 reference`() {
        assertTrue(EuIdChecksums.isValidBulgarianEgn("7523169263")) // Wikipedia EGN example
        assertFalse(EuIdChecksums.isValidBulgarianEgn("7523169264"))
    }

    @Test
    fun `Croatia OIB MOD 11,10 reference`() {
        assertTrue(EuIdChecksums.isValidCroatianOib("12345678903")) // canonical test OIB
        assertFalse(EuIdChecksums.isValidCroatianOib("12345678904"))
    }

    @Test
    fun `Bulgarian persona EGN encodes DOB and sex`() {
        for (seed in 1L..8L) {
            val p = io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator.generate(Country.BG, seed)
            val egn = p.nationalId!!
            assertTrue(NationalIdGenerator.isValid(Country.BG, egn), egn)
            assertEquals((p.dateOfBirth.year % 100).toString().padStart(2, '0'), egn.substring(0, 2), egn)
            val male = p.gender == io.github.vadimtoptunov.kassitestdata.core.Gender.MALE
            assertEquals(male, (egn[8] - '0') % 2 == 0, egn) // 9th digit even = male
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
