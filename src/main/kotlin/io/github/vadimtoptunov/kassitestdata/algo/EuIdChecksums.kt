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
