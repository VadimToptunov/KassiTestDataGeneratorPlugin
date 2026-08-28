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

    /** CUSIP (North-American security): 8-char alphanumeric base + "double add double" check digit. */
    fun cusip(rng: Rng, valid: Boolean): String {
        val base = rng.alnums(8)
        val check = Checksums.cusipCheckDigit(base)
        return if (valid) "$base$check" else "$base${(check + 1) % 10}"
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

    // The 23 characters a VIN may use — the full alphabet minus I, O and Q.
    private const val VIN_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789"

    /** VIN (ISO 3779): 17 chars with the position-9 check character (weighted mod-11). */
    fun vin(rng: Rng, valid: Boolean): String {
        val chars = CharArray(17) { VIN_CHARS[rng.int(VIN_CHARS.length)] }
        // Position 9 carries weight 0, so the placeholder above never affects the check character.
        chars[8] = Checksums.vinCheckChar(String(chars))
        if (!valid) chars[8] = if (chars[8] == '0') '1' else '0' // a wrong-but-legal check character
        return String(chars)
    }

    /** ISBN-10: 9 digits + mod-11 check character (which may be 'X'). */
    fun isbn10(rng: Rng, valid: Boolean): String {
        val body = rng.digitsNonZeroLead(9)
        val check = Checksums.isbn10CheckChar(body)
        if (valid) return "$body$check"
        return "$body${if (check == '0') '1' else '0'}" // a wrong check character
    }

    /** ISBN-13: a 978/979 prefix + 9 digits + EAN-13 check digit. */
    fun isbn13(rng: Rng, valid: Boolean): String {
        val first12 = (if (rng.boolean()) "978" else "979") + rng.digits(9)
        val check = EuIdChecksums.ean13Check(first12)
        return if (valid) "$first12$check" else "$first12${(check + 1) % 10}"
    }

    /** ICCID (SIM card): 19 digits — telecom prefix 89 + serial + Luhn check. */
    fun iccid(rng: Rng, valid: Boolean): String {
        val body = "89" + rng.digits(16) // 18-digit body; ICCIDs are assigned under the 89 industry code
        val check = Checksums.luhnCheckDigit(body)
        return if (valid) "$body$check" else "$body${(check + 1) % 10}"
    }

    /** ISO 6346 shipping-container code: 3-letter owner + category U + 6-digit serial + check digit. */
    fun containerCode(rng: Rng, valid: Boolean): String {
        val body = rng.upperLetters(3) + "U" + rng.digits(6)
        val check = Checksums.iso6346CheckDigit(body)
        return if (valid) "$body$check" else "$body${(check + 1) % 10}"
    }
}
