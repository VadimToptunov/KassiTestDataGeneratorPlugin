package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.algo.EuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * Country-agnostic identifiers with real checksums: ISIN (securities), IMEI (devices), EAN-13 (products).
 * Each returns a spec-valid value or (when [valid] is false) one that fails its checksum.
 */
object IdentifierGenerator {

    private val ISIN_PREFIXES = listOf("US", "GB", "DE", "FR", "NL", "CH", "JP", "AU", "CA", "XS")

    /** ISIN (ISO 6166): 2-letter country + 9-char NSIN + Luhn check over the letter-expanded body. */
    fun isin(rng: Rng, valid: Boolean): String {
        val body = rng.pick(ISIN_PREFIXES) + rng.alnums(9)
        val check = Checksums.isinCheckDigit(body)
        return if (valid) "$body$check" else "$body${(check + 1) % 10}"
    }

    /** IMEI: 15 digits — 14-digit body (TAC + serial) + Luhn check. */
    fun imei(rng: Rng, valid: Boolean): String {
        val first14 = rng.digitsNonZeroLead(14)
        val check = Checksums.luhnCheckDigit(first14)
        return if (valid) "$first14$check" else "$first14${(check + 1) % 10}"
    }

    /** EAN-13 / GTIN-13: 12 digits + EAN-13 check digit. */
    fun ean13(rng: Rng, valid: Boolean): String {
        val first12 = rng.digitsNonZeroLead(12)
        val check = EuIdChecksums.ean13Check(first12)
        return if (valid) "$first12$check" else "$first12${(check + 1) % 10}"
    }
}
