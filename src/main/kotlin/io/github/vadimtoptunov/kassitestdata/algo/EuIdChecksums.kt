package io.github.vadimtoptunov.kassitestdata.algo

/**
 * Checksum algorithms for additional European national-ID, VAT and business identifiers (v1.2).
 * Each is an independent implementation of the published national algorithm; generators produce
 * values that satisfy these, and unit tests anchor them to known reference values.
 */
object EuIdChecksums {

    // ---- Spain — DNI / NIE (control letter, mod 23) ----
    private const val DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE"

    fun spanishControlLetter(number: Int): Char = DNI_LETTERS[number % 23]

    /** DNI: 8 digits + letter. NIE: X/Y/Z + 7 digits + letter (X→0, Y→1, Z→2). */
    fun isValidSpanishDni(value: String): Boolean {
        val s = value.uppercase()
        if (!Regex("^[XYZ]?[0-9]{7,8}[A-Z]$").matches(s)) return false
        val body = s.dropLast(1)
        val numeric = when (body[0]) {
            'X' -> "0" + body.substring(1)
            'Y' -> "1" + body.substring(1)
            'Z' -> "2" + body.substring(1)
            else -> body
        }
        if (body[0] in "XYZ" && body.length != 8) return false
        if (body[0] !in "XYZ" && body.length != 8) return false
        if (!numeric.all { it in '0'..'9' }) return false
        return spanishControlLetter(numeric.toInt()) == s.last()
    }

    // ---- Portugal — NIF / NIPC (mod 11) ----
    fun portugueseNifCheckDigit(first8: String): Int {
        var sum = 0
        for (i in 0..7) sum += (first8[i] - '0') * (9 - i)
        val c = 11 - (sum % 11)
        return if (c >= 10) 0 else c
    }

    fun isValidPortugueseNif(value: String): Boolean {
        if (value.length != 9 || !value.all { it in '0'..'9' }) return false
        return portugueseNifCheckDigit(value.substring(0, 8)) == (value[8] - '0')
    }

    // ---- Poland — PESEL (national ID, encodes DOB + sex) ----
    private val PESEL_WEIGHTS = intArrayOf(1, 3, 7, 9, 1, 3, 7, 9, 1, 3)

    fun peselCheckDigit(first10: String): Int {
        var sum = 0
        for (i in 0..9) sum += (first10[i] - '0') * PESEL_WEIGHTS[i]
        return (10 - (sum % 10)) % 10
    }

    fun isValidPesel(value: String): Boolean {
        if (value.length != 11 || !value.all { it in '0'..'9' }) return false
        return peselCheckDigit(value.substring(0, 10)) == (value[10] - '0')
    }

    // ---- Poland — NIP (VAT, weighted mod 11) ----
    private val NIP_WEIGHTS = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)

    /** The NIP check digit, or null if the weighted sum ≡ 10 (mod 11) — such a NIP is never issued. */
    fun nipCheckDigit(first9: String): Int? {
        var sum = 0
        for (i in 0..8) sum += (first9[i] - '0') * NIP_WEIGHTS[i]
        val c = sum % 11
        return if (c == 10) null else c
    }

    fun isValidNip(value: String): Boolean {
        if (value.length != 10 || !value.all { it in '0'..'9' }) return false
        return nipCheckDigit(value.substring(0, 9)) == (value[9] - '0')
    }

    // ---- Belgium — VAT (mod 97) ----
    fun belgianVatCheck(first8: String): Int = 97 - (first8.toInt() % 97)

    fun isValidBelgianVat(value: String): Boolean {
        if (value.length != 10 || !value.all { it in '0'..'9' }) return false
        return belgianVatCheck(value.substring(0, 8)) == value.substring(8).toInt()
    }

    // ---- France — NIR / INSEE (national ID, mod 97 key; encodes sex + DOB) ----
    /** NIR key = 97 − (13-digit body mod 97), always in 1..97. Numeric NIRs only (Corsica 2A/2B unsupported). */
    fun frenchNirKey(first13: String): Int = 97 - Checksums.mod97(first13)

    fun isValidFrenchNir(value: String): Boolean {
        if (value.length != 15 || !value.all { it in '0'..'9' }) return false
        if (value[0] != '1' && value[0] != '2') return false
        return frenchNirKey(value.substring(0, 13)) == value.substring(13).toInt()
    }

    // ---- Belgium — National Register Number (mod 97; +2000000000 for births from 2000; encodes DOB + sex) ----
    fun belgianNationalNumberCheck(first9: String, bornFrom2000: Boolean): Int {
        val base = (if (bornFrom2000) "2$first9" else first9).toLong()
        return 97 - (base % 97).toInt()
    }

    fun isValidBelgianNationalNumber(value: String): Boolean {
        if (value.length != 11 || !value.all { it in '0'..'9' }) return false
        val first9 = value.substring(0, 9)
        val provided = value.substring(9).toInt()
        // The number itself doesn't say whether the person was born before or from 2000, so accept either.
        return belgianNationalNumberCheck(first9, false) == provided ||
            belgianNationalNumberCheck(first9, true) == provided
    }

    // ---- Sweden — personnummer (10 digits, Luhn over the first 9; encodes DOB + sex) ----
    fun isValidSwedishPersonnummer(value: String): Boolean =
        value.length == 10 && value.all { it in '0'..'9' } && Checksums.isLuhnValid(value)

    // ---- Finland — HETU (personal identity code, mod 31 check char; encodes DOB + sex) ----
    private const val HETU_CHECK = "0123456789ABCDEFHJKLMNPRSTUVWXY" // 31 symbols
    private const val HETU_SIGNS = "+-ABCDEFYXWVU" // century markers (1800s / 2000s / 1900s)

    /** Check char for the 9-digit `DDMMYY` + individual number string. */
    fun finnishHetuCheckChar(dateAndIndividual: String): Char = HETU_CHECK[(dateAndIndividual.toLong() % 31).toInt()]

    fun isValidFinnishHetu(value: String): Boolean {
        // DDMMYY (6) + century sign (1) + individual (3) + check (1)
        if (value.length != 11) return false
        val date = value.substring(0, 6)
        val individual = value.substring(7, 10)
        if (!date.all { it in '0'..'9' } || !individual.all { it in '0'..'9' }) return false
        if (value[6] !in HETU_SIGNS) return false
        return finnishHetuCheckChar(date + individual) == value[10]
    }

    // ---- Norway — fødselsnummer (11 digits, two mod-11 check digits; encodes DOB + sex) ----
    private val NO_WEIGHTS_1 = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
    private val NO_WEIGHTS_2 = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

    /** The two check digits for a 9-digit `DDMMYY` + individual body, or null if either would be 10 (never issued). */
    fun norwegianCheckDigits(first9: String): String? {
        var s1 = 0
        for (i in 0..8) s1 += (first9[i] - '0') * NO_WEIGHTS_1[i]
        val k1 = (11 - (s1 % 11)) % 11
        if (k1 == 10) return null
        val first10 = first9 + k1
        var s2 = 0
        for (i in 0..9) s2 += (first10[i] - '0') * NO_WEIGHTS_2[i]
        val k2 = (11 - (s2 % 11)) % 11
        if (k2 == 10) return null
        return "$k1$k2"
    }

    fun isValidNorwegianFnr(value: String): Boolean {
        if (value.length != 11 || !value.all { it in '0'..'9' }) return false
        return norwegianCheckDigits(value.substring(0, 9)) == value.substring(9)
    }

    // ---- France — VAT / TVA (2-digit key over a Luhn-valid SIREN, mod 97) ----
    fun frenchVatKey(siren: String): Int = (12 + 3 * (siren.toLong() % 97)).toInt() % 97

    fun isValidFrenchVat(value: String): Boolean {
        // key (2) + SIREN (9, Luhn-valid)
        if (value.length != 11 || !value.all { it in '0'..'9' }) return false
        val siren = value.substring(2)
        if (!Checksums.isLuhnValid(siren)) return false
        return frenchVatKey(siren) == value.substring(0, 2).toInt()
    }

    // ---- Spain — CIF (company tax ID); control per the standard, a digit for type A ----
    /** Control value 0..9 for the 7-digit body: even positions summed, odd positions doubled + cross-summed. */
    fun spanishCifControl(sevenDigits: String): Int {
        var sum = 0
        for (i in 0..6) {
            val d = sevenDigits[i] - '0'
            if (i % 2 == 0) { // 1st, 3rd, 5th, 7th digit — double and cross-sum
                val doubled = d * 2
                sum += doubled / 10 + doubled % 10
            } else {
                sum += d
            }
        }
        return (10 - sum % 10) % 10
    }

    /** Type-A CIF (Sociedad Anónima): letter A + 7 digits + a digit control. */
    fun isValidSpanishCifTypeA(value: String): Boolean {
        if (value.length != 9 || value[0] != 'A') return false
        val digits = value.substring(1, 8)
        if (!digits.all { it in '0'..'9' } || value[8] !in '0'..'9') return false
        return spanishCifControl(digits) == (value[8] - '0')
    }

    // ---- LEI — Legal Entity Identifier (ISO 17442 / ISO 7064 MOD 97-10) ----
    private fun toNumeric(s: String): String = buildString {
        for (c in s.uppercase()) when {
            c in '0'..'9' -> append(c)
            c in 'A'..'Z' -> append((c - 'A' + 10).toString())
            else -> throw IllegalArgumentException("Illegal LEI char '$c'")
        }
    }

    /** The 2 LEI check digits for an 18-char base (LOU prefix + reserved + entity). */
    fun leiCheckDigits(base18: String): String {
        val check = 98 - Checksums.mod97(toNumeric(base18 + "00"))
        return check.toString().padStart(2, '0')
    }

    fun isValidLei(value: String): Boolean {
        val s = value.uppercase()
        if (!Regex("^[A-Z0-9]{18}[0-9]{2}$").matches(s)) return false
        return Checksums.mod97(toNumeric(s)) == 1
    }
}
