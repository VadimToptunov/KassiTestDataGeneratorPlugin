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
        Country.FR to Scheme("NIR (INSEE)", Validation.CHECKSUM),
        Country.BE to Scheme("National No.", Validation.CHECKSUM),
        Country.SE to Scheme("Personnummer", Validation.CHECKSUM),
        Country.FI to Scheme("HETU", Validation.CHECKSUM),
        Country.NO to Scheme("Fødselsnummer", Validation.CHECKSUM),
        Country.IT to Scheme("Codice Fiscale", Validation.CHECKSUM),
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
        surname: String? = null,
        givenName: String? = null,
    ): String = when (country) {
        Country.NL -> bsn(rng, valid)
        Country.DE -> germanIdCard(rng, valid)
        Country.AU -> tfn(rng, valid)
        Country.GB -> nino(rng, valid)
        Country.CY -> cyprusId(rng, valid)
        Country.ES -> spanishDni(rng, valid)
        Country.PT -> portugueseNif(rng, valid)
        Country.PL -> pesel(rng, valid, birth, gender)
        Country.FR -> frenchNir(rng, valid, birth, gender)
        Country.BE -> belgianNationalNumber(rng, valid, birth, gender)
        Country.SE -> swedishPersonnummer(rng, valid, birth, gender)
        Country.FI -> finnishHetu(rng, valid, birth, gender)
        Country.NO -> norwegianFnr(rng, valid, birth, gender)
        Country.IT -> italianCodiceFiscale(rng, valid, birth, gender, surname, givenName)
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
        Country.FR -> EuIdChecksums.isValidFrenchNir(value)
        Country.BE -> EuIdChecksums.isValidBelgianNationalNumber(value)
        Country.SE -> EuIdChecksums.isValidSwedishPersonnummer(value)
        Country.FI -> EuIdChecksums.isValidFinnishHetu(value)
        Country.NO -> EuIdChecksums.isValidNorwegianFnr(value)
        Country.IT -> EuIdChecksums.isValidItalianCf(value)
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

    // --- FR NIR / INSEE (13 digits + 2 key; sex + YY + MM + dept + commune + order) ---
    private fun frenchNir(rng: Rng, valid: Boolean, birth: LocalDate?, gender: Gender?): String {
        val sex = gender ?: randomGender(rng)
        val dob = birth ?: randomDob(rng)
        val sexDigit = if (sex == Gender.MALE) '1' else '2'
        val yy = (dob.year % 100).toString().padStart(2, '0')
        val mm = dob.monthValue.toString().padStart(2, '0')
        val dept = rng.intInRange(1, 95).let { if (it == 20) 19 else it } // mainland only (skip Corsica 2A/2B)
            .toString().padStart(2, '0')
        val first13 = "$sexDigit$yy$mm$dept${rng.digits(3)}${rng.digitsNonZeroLead(3)}"
        val key = EuIdChecksums.frenchNirKey(first13)
        val keyStr = key.toString().padStart(2, '0')
        return if (valid) first13 + keyStr else first13 + wrongTwoDigit(key)
    }

    // --- BE National Register Number (11 digits; YYMMDD + serial + mod-97 check) ---
    private fun belgianNationalNumber(rng: Rng, valid: Boolean, birth: LocalDate?, gender: Gender?): String {
        val sex = gender ?: randomGender(rng)
        val dob = birth ?: randomDob(rng)
        val yy = (dob.year % 100).toString().padStart(2, '0')
        val mm = dob.monthValue.toString().padStart(2, '0')
        val dd = dob.dayOfMonth.toString().padStart(2, '0')
        // Serial 001..996; parity encodes sex (odd = male, even = female).
        val base = rng.intInRange(1, 498)
        val serial = (if (sex == Gender.MALE) 2 * base - 1 else 2 * base).toString().padStart(3, '0')
        val first9 = "$yy$mm$dd$serial"
        val check = EuIdChecksums.belgianNationalNumberCheck(first9, bornFrom2000 = dob.year >= 2000)
        if (valid) return first9 + check.toString().padStart(2, '0')
        // isValid() accepts either birth era, so the invalid trailing check must match neither.
        val c0 = EuIdChecksums.belgianNationalNumberCheck(first9, bornFrom2000 = false)
        val c1 = EuIdChecksums.belgianNationalNumberCheck(first9, bornFrom2000 = true)
        var wrong = (check % 97) + 1
        while (wrong == c0 || wrong == c1) wrong = (wrong % 97) + 1
        return first9 + wrong.toString().padStart(2, '0')
    }

    // --- SE personnummer (10 digits; YYMMDD + birth number + Luhn) ---
    private fun swedishPersonnummer(rng: Rng, valid: Boolean, birth: LocalDate?, gender: Gender?): String {
        val sex = gender ?: randomGender(rng)
        val dob = birth ?: randomDob(rng)
        val yy = (dob.year % 100).toString().padStart(2, '0')
        val mm = dob.monthValue.toString().padStart(2, '0')
        val dd = dob.dayOfMonth.toString().padStart(2, '0')
        // 9th digit parity encodes sex (odd = male, even = female).
        val sexDigit = if (sex == Gender.MALE) 2 * rng.intInRange(0, 4) + 1 else 2 * rng.intInRange(0, 4)
        val first9 = "$yy$mm$dd${rng.digits(2)}$sexDigit"
        val check = Checksums.luhnCheckDigit(first9)
        return if (valid) first9 + check else first9 + ((check + 1) % 10)
    }

    // --- FI HETU (DDMMYY + century sign + individual + mod-31 check char) ---
    private fun finnishHetu(rng: Rng, valid: Boolean, birth: LocalDate?, gender: Gender?): String {
        val sex = gender ?: randomGender(rng)
        val dob = birth ?: randomDob(rng)
        val dd = dob.dayOfMonth.toString().padStart(2, '0')
        val mm = dob.monthValue.toString().padStart(2, '0')
        val yy = (dob.year % 100).toString().padStart(2, '0')
        val sign = when (dob.year / 100) { 18 -> '+'; 20 -> 'A'; else -> '-' }
        // Individual number 002..899; parity encodes sex (odd = male, even = female).
        val base = rng.intInRange(1, 449)
        val individual = (if (sex == Gender.MALE) 2 * base + 1 else 2 * base).toString().padStart(3, '0')
        val check = EuIdChecksums.finnishHetuCheckChar(dd + mm + yy + individual)
        val body = "$dd$mm$yy$sign$individual"
        return if (valid) "$body$check" else body + if (check == '0') '1' else '0'
    }

    // --- NO fødselsnummer (DDMMYY + individual + two mod-11 check digits) ---
    private fun norwegianFnr(rng: Rng, valid: Boolean, birth: LocalDate?, gender: Gender?): String {
        val sex = gender ?: randomGender(rng)
        val dob = birth ?: randomDob(rng)
        val dd = dob.dayOfMonth.toString().padStart(2, '0')
        val mm = dob.monthValue.toString().padStart(2, '0')
        val yy = (dob.year % 100).toString().padStart(2, '0')
        while (true) {
            // 9th digit parity encodes sex (odd = male, even = female).
            val parity = if (sex == Gender.MALE) 2 * rng.intInRange(0, 4) + 1 else 2 * rng.intInRange(0, 4)
            val first9 = "$dd$mm$yy${rng.digits(2)}$parity"
            val checks = EuIdChecksums.norwegianCheckDigits(first9) ?: continue // a check digit hit 10 — retry
            if (valid) return first9 + checks
            val wrong = ((checks.toInt() + 1) % 100).toString().padStart(2, '0')
            return first9 + wrong
        }
    }

    // --- IT Codice Fiscale (6 name letters + DOB/sex + Belfiore place code + control char) ---
    private const val CF_MONTHS = "ABCDEHLMPRST"
    // A handful of real Belfiore codes (major cities): Roma, Milano, Napoli, Torino, Firenze, Bologna, Genova, Palermo.
    private val CF_PLACE_CODES = listOf("H501", "F205", "F839", "L219", "D612", "A944", "D969", "G273")

    private fun italianCodiceFiscale(
        rng: Rng,
        valid: Boolean,
        birth: LocalDate?,
        gender: Gender?,
        surname: String?,
        givenName: String?,
    ): String {
        val sex = gender ?: randomGender(rng)
        val dob = birth ?: randomDob(rng)
        val sn = cfSurnameCode(surname ?: rng.upperLetters(3))
        val gn = cfGivenNameCode(givenName ?: rng.upperLetters(3))
        val yy = (dob.year % 100).toString().padStart(2, '0')
        val monthChar = CF_MONTHS[dob.monthValue - 1]
        val day = (dob.dayOfMonth + if (sex == Gender.FEMALE) 40 else 0).toString().padStart(2, '0')
        val first15 = "$sn$gn$yy$monthChar$day${rng.pick(CF_PLACE_CODES)}"
        val check = EuIdChecksums.italianCfCheckChar(first15)
        return if (valid) "$first15$check" else first15 + if (check == 'A') 'B' else 'A'
    }

    private fun cfConsonants(name: String): String = name.filter { it in "BCDFGHJKLMNPQRSTVWXYZ" }
    private fun cfVowels(name: String): String = name.filter { it in "AEIOU" }

    /** Surname code: consonants then vowels, first three, padded with X. */
    private fun cfSurnameCode(name: String): String {
        val n = name.uppercase().filter { it in 'A'..'Z' }
        return (cfConsonants(n) + cfVowels(n) + "XXX").substring(0, 3)
    }

    /** Given-name code: with 4+ consonants take the 1st, 3rd and 4th; otherwise consonants then vowels, padded. */
    private fun cfGivenNameCode(name: String): String {
        val n = name.uppercase().filter { it in 'A'..'Z' }
        val consonants = cfConsonants(n)
        val picked = if (consonants.length >= 4) "${consonants[0]}${consonants[2]}${consonants[3]}" else consonants
        return (picked + cfVowels(n) + "XXX").substring(0, 3)
    }

    private fun randomGender(rng: Rng): Gender = if (rng.boolean()) Gender.MALE else Gender.FEMALE

    private fun randomDob(rng: Rng): LocalDate =
        LocalDate.of(rng.intInRange(1950, 2005), rng.intInRange(1, 12), rng.intInRange(1, 28))

    private fun corruptLastDigit(number: String): String {
        val last = number.last() - '0'
        return number.dropLast(1) + ((last + 1) % 10)
    }

    /** A 2-digit string that differs from [check] (used to build an invalid trailing check). */
    private fun wrongTwoDigit(check: Int): String = ((check % 97) + 1).toString().padStart(2, '0')
}
