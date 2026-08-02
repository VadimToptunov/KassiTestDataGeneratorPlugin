package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Gender
import io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.MrzGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MrzGeneratorTest {

    // Reference values from the ICAO 9303 specimen line 2:
    // L898902C36UTO7408122F1204159ZE184226B<<<<<10
    @Test
    fun `ICAO 9303 reference check digits`() {
        assertEquals(6, MrzGenerator.checkDigit("L898902C3"))
        assertEquals(2, MrzGenerator.checkDigit("740812"))
        assertEquals(9, MrzGenerator.checkDigit("120415"))
        assertEquals(1, MrzGenerator.checkDigit("ZE184226B<<<<<"))
        // Composite: passport(9)+chk + dob(6)+chk + expiry(6)+chk + personal(14)+chk
        assertEquals(0, MrzGenerator.checkDigit("L898902C36" + "7408122" + "1204159ZE184226B<<<<<1"))
    }

    @Test
    fun `generated passport MRZ is valid for every country, invalid variant fails`() {
        val rng = Rng(2024L)
        for (country in Country.entries) {
            repeat(10) {
                val mrz = MrzGenerator.td3(country, rng, valid = true)
                val lines = mrz.split("\n")
                assertEquals(2, lines.size, country.code)
                assertTrue(lines.all { it.length == 44 }, "${country.code} line length: $mrz")
                assertTrue(lines[1].substring(10, 13) == MrzGenerator.alpha3(country), "${country.code} nationality")
                assertTrue(MrzGenerator.isValidTd3(mrz), "${country.code} MRZ should validate:\n$mrz")

                assertFalse(MrzGenerator.isValidTd3(MrzGenerator.td3(country, rng, valid = false)), country.code)
            }
        }
    }

    @Test
    fun `MRZ encodes name, nationality, DOB and sex`() {
        val mrz = MrzGenerator.td3(
            Country.DE, Rng(1L), valid = true,
            birth = java.time.LocalDate.of(1974, 8, 12), gender = Gender.FEMALE,
            surname = "Eriksson", givenNames = "Anna Maria",
        )
        val lines = mrz.split("\n")
        assertTrue(lines[0].startsWith("P<DEUERIKSSON<<ANNA<MARIA"), lines[0])
        assertEquals("DEU", lines[1].substring(10, 13))
        assertEquals("740812", lines[1].substring(13, 19)) // DOB
        assertEquals('F', lines[1][20])                    // sex
        assertTrue(MrzGenerator.isValidTd3(mrz), mrz)
    }

    @Test
    fun `persona passport MRZ is valid and coherent with DOB and sex`() {
        for (country in listOf(Country.FR, Country.SA, Country.BR, Country.TR)) {
            val p = PersonaGenerator.generate(country, seed = 42L)
            val lines = p.passportMrz.split("\n")
            assertTrue(MrzGenerator.isValidTd3(p.passportMrz), "${country.code}: ${p.passportMrz}")
            assertEquals(MrzGenerator.alpha3(country), lines[1].substring(10, 13))
            val yymmdd = (p.dateOfBirth.year % 100).toString().padStart(2, '0') +
                p.dateOfBirth.monthValue.toString().padStart(2, '0') +
                p.dateOfBirth.dayOfMonth.toString().padStart(2, '0')
            assertEquals(yymmdd, lines[1].substring(13, 19), country.code)
            assertEquals(if (p.gender == Gender.MALE) 'M' else 'F', lines[1][20])
        }
    }
}
