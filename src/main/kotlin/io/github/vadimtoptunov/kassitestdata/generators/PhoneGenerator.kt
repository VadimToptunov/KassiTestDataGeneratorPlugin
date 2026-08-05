package io.github.vadimtoptunov.kassitestdata.generators

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * Valid test phone numbers per country, backed by Google's libphonenumber metadata (offline, bundled —
 * per-country numbering plans are not something to guess). A country's example number is varied in its
 * subscriber part and re-validated, so results differ while staying valid; the invalid variant fails validation.
 */
object PhoneGenerator {

    private val util = PhoneNumberUtil.getInstance()

    /** Whether libphonenumber has a usable example number for this country's region. */
    fun isSupported(country: Country): Boolean = exampleFor(country) != null

    /** An E.164 number (e.g. +447911123456). libphonenumber region codes are ISO 3166-1 alpha-2 == [Country.code]. */
    fun generate(country: Country, rng: Rng, valid: Boolean = true): String {
        val example = exampleFor(country) ?: return "-"
        val number = if (valid) varyValid(example, rng) else invalidate(example)
        return util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
    }

    private fun exampleFor(country: Country): PhoneNumber? =
        util.getExampleNumberForType(country.code, PhoneNumberUtil.PhoneNumberType.MOBILE)
            ?: util.getExampleNumber(country.code)

    private fun varyValid(example: PhoneNumber, rng: Rng): PhoneNumber {
        val nn = example.nationalNumber.toString()
        if (nn.length <= 4) return example
        repeat(6) {
            val candidate = rebuild(nn.dropLast(4) + rng.digits(4), example)
            if (util.isValidNumber(candidate)) return candidate
        }
        return example // the example itself is always valid
    }

    private fun invalidate(example: PhoneNumber): PhoneNumber {
        // Appending digits makes the national number the wrong length for its plan -> invalid.
        var nn = example.nationalNumber.toString() + "99"
        var candidate = rebuild(nn, example)
        var guard = 0
        while (util.isValidNumber(candidate) && guard++ < 5) {
            nn += "9"
            candidate = rebuild(nn, example)
        }
        return candidate
    }

    private fun rebuild(nationalNumber: String, template: PhoneNumber): PhoneNumber {
        val number = PhoneNumber().setCountryCode(template.countryCode).setNationalNumber(nationalNumber.toLong())
        if (template.isItalianLeadingZero) {
            number.setItalianLeadingZero(true)
            number.setNumberOfLeadingZeros(template.numberOfLeadingZeros)
        }
        return number
    }
}
