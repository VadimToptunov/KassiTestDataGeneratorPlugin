package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.algo.EuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Gender
import io.github.vadimtoptunov.kassitestdata.core.Rng
import java.time.LocalDate

/**
 * National / personal identifier — a per-country abstraction. Where a public checksum exists
 * it is enforced (NL BSN 11-proef, DE ID-card 7-3-1, AU TFN weighted mod-11); GB NINo and CY are
 * format schemes with no public checksum. Additional national schemes roll out per the roadmap.
 */
object NationalIdGenerator {

    enum class Validation { CHECKSUM, FORMAT }

    data class Scheme(val label: String, val validation: Validation)

    val supported: Map<Country, Scheme> = mapOf(
        Country.NL to Scheme("BSN", Validation.CHECKSUM),
        Country.DE to Scheme("Personalausweis No.", Validation.CHECKSUM),
        Country.AU to Scheme("TFN", Validation.CHECKSUM),
        Country.GB to Scheme("NINo", Validation.FORMAT),
        Country.CY to Scheme("ID card No.", Validation.FORMAT),
        Country.ES to Scheme("DNI/NIE", Validation.CHECKSUM),
        Country.PT to Scheme("NIF", Validation.CHECKSUM),
        Country.PL to Scheme("PESEL", Validation.CHECKSUM),
    )

    /**
     * [birth] / [gender] are honoured by schemes that encode them (e.g. PL PESEL) so a persona's
     * national ID stays consistent with its date of birth; other schemes ignore them.
     */
    fun generate(
        country: Country,
        rng: Rng,
        valid: Boolean = true,
        birth: LocalDate? = null,
        gender: Gender? = null,
    ): String = when (country) {
        Country.NL -> bsn(rng, valid)
        Country.DE -> germanIdCard(rng, valid)
        Country.AU -> tfn(rng, valid)
        Country.GB -> nino(rng, valid)
        Country.CY -> cyprusId(rng, valid)
        Country.ES -> spanishDni(rng, valid)
        Country.PT -> portugueseNif(rng, valid)
        Country.PL -> pesel(rng, valid, birth, gender)
        else -> throw IllegalArgumentException("No national ID scheme for ${country.code} in v1")
    }

    fun isValid(country: Country, value: String): Boolean = when (country) {
        Country.NL -> Checksums.isValidElevenProof(value)
        Country.DE -> Checksums.isValidGermanIdCard(value)
        Country.AU -> Checksums.isValidTfn(value)
        Country.GB -> isValidNino(value)
        Country.CY -> value.length == 10 && value.all { it in '0'..'9' }
        Country.ES -> EuIdChecksums.isValidSpanishDni(value)
        Country.PT -> EuIdChecksums.isValidPortugueseNif(value)
        Country.PL -> EuIdChecksums.isValidPesel(value)
        else -> false
    }

    // --- NL BSN (9-digit 11-proef) ---
    private fun bsn(rng: Rng, valid: Boolean): String {
        while (true) {
            val first8 = rng.digitsNonZeroLead(8)
            val weights = intArrayOf(9, 8, 7, 6, 5, 4, 3, 2)
            var sum8 = 0
            for (i in 0..7) sum8 += (first8[i] - '0') * weights[i]
            val d9 = sum8 % 11 // last weight is -1, so need d9 ≡ sum8 (mod 11)
            if (d9 == 10) continue
            val bsn = first8 + d9
            if (bsn == "000000000") continue
            return if (valid) bsn else corruptLastDigit(bsn)
        }
    }

    // --- DE ID card number (9-char serial + ICAO 7-3-1 check digit) ---
    private const val DE_ID_ALPHABET = "0123456789CFGHJKLMNPRTVWXYZ"
    private fun germanIdCard(rng: Rng, valid: Boolean): String {
        val serial = buildString { repeat(9) { append(DE_ID_ALPHABET[rng.int(DE_ID_ALPHABET.length)]) } }
        val check = Checksums.icao731CheckDigit(serial)
        return if (valid) serial + check else serial + ((check + 1) % 10)
    }

    // --- AU TFN (9-digit weighted mod-11) ---
    private fun tfn(rng: Rng, valid: Boolean): String {
        while (true) {
            val first8 = rng.digitsNonZeroLead(8)
            var sum8 = 0
            for (i in 0..7) sum8 += (first8[i] - '0') * Checksums.TFN_WEIGHTS[i]
            val d9 = sum8 % 11 // last weight is 10 ≡ -1 (mod 11), so d9 ≡ sum8 (mod 11)
            if (d9 == 10) continue
            val tfn = first8 + d9
            return if (valid) tfn else corruptLastDigit(tfn)
        }
    }

    // --- GB NINo (AA 12 34 56 A) ---
    private val NINO_FIRST = ('A'..'Z').filter { it !in "DFIQUV" }
    private val NINO_SECOND = ('A'..'Z').filter { it !in "DFIOQUV" }
    private val NINO_DISALLOWED_PREFIX = setOf("BG", "GB", "KN", "NK", "NT", "TN", "ZZ")
    private val NINO_SUFFIX = listOf('A', 'B', 'C', 'D')

    private fun nino(rng: Rng, valid: Boolean): String {
        if (!valid) return "QQ${rng.digits(6)}Z" // Q disallowed, Z suffix invalid
        var prefix: String
        do {
            prefix = "${rng.pick(NINO_FIRST)}${rng.pick(NINO_SECOND)}"
        } while (prefix in NINO_DISALLOWED_PREFIX)
        return prefix + rng.digits(6) + rng.pick(NINO_SUFFIX)
    }

    private val NINO_PATTERN = Regex("^[A-Z]{2}[0-9]{6}[A-D]$")
    fun isValidNino(value: String): Boolean {
        if (!NINO_PATTERN.matches(value)) return false
        if (value[0] !in NINO_FIRST || value[1] !in NINO_SECOND) return false
        return value.substring(0, 2) !in NINO_DISALLOWED_PREFIX
    }

    // --- CY ID card number (numeric, no public checksum) ---
    private fun cyprusId(rng: Rng, valid: Boolean): String =
        if (valid) rng.digitsNonZeroLead(10) else rng.digits(6) // wrong length when invalid

    // --- ES DNI (8 digits + control letter, mod 23) ---
    private fun spanishDni(rng: Rng, valid: Boolean): String {
        val number = rng.intInRange(0, 99_999_999)
        val body = number.toString().padStart(8, '0')
        val letter = EuIdChecksums.spanishControlLetter(number)
        val wrong = if (letter == 'Z') 'A' else letter + 1
        return if (valid) body + letter else body + wrong
    }

    // --- PT NIF (9 digits, mod 11) ---
    private fun portugueseNif(rng: Rng, valid: Boolean): String {
        val first8 = rng.digitsNonZeroLead(8)
        val check = EuIdChecksums.portugueseNifCheckDigit(first8)
        return if (valid) first8 + check else first8 + ((check + 1) % 10)
    }

    // --- PL PESEL (11 digits; encodes DOB + sex) ---
    private fun pesel(rng: Rng, valid: Boolean, birth: LocalDate?, gender: Gender?): String {
        val sex = gender ?: if (rng.boolean()) Gender.MALE else Gender.FEMALE
        val dob = birth ?: LocalDate.of(rng.intInRange(1950, 2005), rng.intInRange(1, 12), rng.intInRange(1, 28))
        val yy = (dob.year % 100).toString().padStart(2, '0')
        val monthOffset = when (dob.year / 100) {
            18 -> 80; 19 -> 0; 20 -> 20; 21 -> 40; 22 -> 60; else -> 0
        }
        val mm = (dob.monthValue + monthOffset).toString().padStart(2, '0')
        val dd = dob.dayOfMonth.toString().padStart(2, '0')
        // sex is the 10th digit: odd = male, even = female
        val sexDigit = if (sex == Gender.MALE) 2 * rng.intInRange(0, 4) + 1 else 2 * rng.intInRange(0, 4)
        val first10 = yy + mm + dd + rng.digits(3) + sexDigit
        val check = EuIdChecksums.peselCheckDigit(first10)
        return if (valid) first10 + check else first10 + ((check + 1) % 10)
    }

    private fun corruptLastDigit(number: String): String {
        val last = number.last() - '0'
        return number.dropLast(1) + ((last + 1) % 10)
    }
}
