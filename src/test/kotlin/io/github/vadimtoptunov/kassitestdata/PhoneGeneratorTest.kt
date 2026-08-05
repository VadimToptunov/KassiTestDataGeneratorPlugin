package io.github.vadimtoptunov.kassitestdata

import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.PhoneGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhoneGeneratorTest {

    private val util = PhoneNumberUtil.getInstance()

    private fun isValid(e164: String, region: String): Boolean =
        runCatching { util.isValidNumber(util.parse(e164, region)) }.getOrDefault(false)

    @Test
    fun `generated numbers are valid per libphonenumber, invalid variants are not`() {
        val rng = Rng(2024L)
        for (country in Country.entries.filter { PhoneGenerator.isSupported(it) }) {
            repeat(5) {
                val valid = PhoneGenerator.generate(country, rng, valid = true)
                assertTrue(valid.startsWith("+"), "${country.code} E.164: $valid")
                assertTrue(isValid(valid, country.code), "${country.code} should be valid: $valid")

                val invalid = PhoneGenerator.generate(country, rng, valid = false)
                assertFalse(isValid(invalid, country.code), "${country.code} should be invalid: $invalid")
            }
        }
    }

    @Test
    fun `numbers carry the expected country calling code`() {
        val rng = Rng(7L)
        assertTrue(PhoneGenerator.generate(Country.GB, rng, valid = true).startsWith("+44"))
        assertTrue(PhoneGenerator.generate(Country.DE, rng, valid = true).startsWith("+49"))
        assertTrue(PhoneGenerator.generate(Country.RU, rng, valid = true).startsWith("+7"))
    }
}
