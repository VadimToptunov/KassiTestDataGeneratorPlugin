package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.algo.EuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * Business / tax identifier — VAT for EU members (VIES presentation + national checksum) and
 * ABN for Australia (no VAT). Each supported country uses its real checksum; the AU case again
 * forces the abstraction (ABN, not VAT).
 */
object TaxIdGenerator {

    data class Scheme(val label: String)

    val supported: Map<Country, Scheme> = mapOf(
        Country.CY to Scheme("VAT"),
        Country.DE to Scheme("VAT (USt-IdNr)"),
        Country.NL to Scheme("VAT (BTW)"),
        Country.GB to Scheme("VAT"),
        Country.AU to Scheme("ABN"),
        Country.PL to Scheme("VAT (NIP)"),
        Country.IT to Scheme("VAT (P.IVA)"),
        Country.BE to Scheme("VAT (BTW/TVA)"),
        Country.PT to Scheme("VAT (NIF)"),
        Country.FR to Scheme("VAT (TVA)"),
        Country.ES to Scheme("VAT (CIF)"),
    )

    /** Full identifier as presented (with country prefix for VAT). */
    fun generate(country: Country, rng: Rng, valid: Boolean = true): String = when (country) {
        Country.CY -> "CY" + cyprusVatBody(rng, valid)
        Country.DE -> "DE" + germanVatBody(rng, valid)
        Country.NL -> "NL" + netherlandsVatBody(rng, valid)
        Country.GB -> "GB" + ukVatBody(rng, valid)
        Country.AU -> abnFormatted(rng, valid)
        Country.PL -> "PL" + polishNip(rng, valid)
        Country.IT -> "IT" + italianVat(rng, valid)
        Country.BE -> "BE" + belgianVat(rng, valid)
        Country.PT -> "PT" + portugueseVat(rng, valid)
        Country.FR -> "FR" + frenchVat(rng, valid)
        Country.ES -> "ES" + spanishCif(rng, valid)
        else -> throw IllegalArgumentException("No tax scheme for ${country.code} in v1")
    }

    // --- PL NIP: 10 digits, weighted mod 11 ---
    fun polishNip(rng: Rng, valid: Boolean): String {
        while (true) {
            val first9 = rng.digitsNonZeroLead(9)
            val check = EuIdChecksums.nipCheckDigit(first9) ?: continue
            return if (valid) first9 + check else first9 + ((check + 1) % 10)
        }
    }

    // --- IT Partita IVA: 11 digits, Luhn ---
    fun italianVat(rng: Rng, valid: Boolean): String {
        val first10 = rng.digitsNonZeroLead(10)
        val check = Checksums.luhnCheckDigit(first10)
        return if (valid) first10 + check else first10 + ((check + 1) % 10)
    }

    // --- BE VAT: 10 digits, mod 97 (enterprise number leads with 0 or 1) ---
    fun belgianVat(rng: Rng, valid: Boolean): String {
        while (true) {
            val first8 = (if (rng.boolean()) "0" else "1") + rng.digits(7)
            if (first8.toInt() % 97 == 0) continue
            val check = EuIdChecksums.belgianVatCheck(first8).toString().padStart(2, '0')
            if (valid) return first8 + check
            var c = check.toInt()
            var body: String
            do {
                c = (c + 1) % 100
                body = first8 + c.toString().padStart(2, '0')
            } while (EuIdChecksums.isValidBelgianVat(body))
            return body
        }
    }

    // --- PT VAT = NIF (9 digits, mod 11) ---
    fun portugueseVat(rng: Rng, valid: Boolean): String {
        val first8 = rng.digitsNonZeroLead(8)
        val check = EuIdChecksums.portugueseNifCheckDigit(first8)
        return if (valid) first8 + check else first8 + ((check + 1) % 10)
    }

    // --- FR TVA: 2-digit key + 9-digit Luhn-valid SIREN ---
    fun frenchVat(rng: Rng, valid: Boolean): String {
        val siren8 = rng.digitsNonZeroLead(8)
        val siren = siren8 + Checksums.luhnCheckDigit(siren8)
        val key = EuIdChecksums.frenchVatKey(siren)
        if (valid) return key.toString().padStart(2, '0') + siren
        // Corrupt the key so it no longer matches the SIREN (the SIREN stays Luhn-valid).
        val wrongKey = (key + 1) % 97
        return wrongKey.toString().padStart(2, '0') + siren
    }

    // --- ES CIF (type A, Sociedad Anónima): 'A' + 7 digits + digit control ---
    fun spanishCif(rng: Rng, valid: Boolean): String {
        val digits = rng.digits(7)
        val control = EuIdChecksums.spanishCifControl(digits)
        return if (valid) "A$digits$control" else "A$digits${(control + 1) % 10}"
    }

    // --- CY VAT: 8 digits + check letter ---
    fun cyprusVatBody(rng: Rng, valid: Boolean): String {
        var digits: String
        do {
            digits = rng.digitsNonZeroLead(8)
        } while (digits.startsWith("12")) // Cyprus VAT must not start with 12
        val letter = Checksums.cyprusVatCheckLetter(digits)
        return if (valid) digits + letter else digits + wrongLetter(letter)
    }

    // --- DE VAT: 9 digits, ISO 7064 MOD 11,10 check digit ---
    fun germanVatBody(rng: Rng, valid: Boolean): String {
        val first8 = rng.digitsNonZeroLead(8)
        val check = Checksums.mod1110CheckDigit(first8)
        return if (valid) first8 + check else first8 + ((check + 1) % 10)
    }

    // --- NL VAT (legacy): 9-digit 11-proef body + "B" + 2-digit suffix ---
    fun netherlandsVatBody(rng: Rng, valid: Boolean): String {
        val nine = elevenProofNine(rng)
        val suffix = rng.intInRange(1, 99).toString().padStart(2, '0')
        return if (valid) "${nine}B$suffix" else "${corruptLastDigit(nine)}B$suffix"
    }

    /** The 9-digit RSIN/BSN-style body used by NL VAT — passes the 11-proef. */
    fun netherlandsVatNineDigits(rng: Rng): String = elevenProofNine(rng)

    // --- GB VAT: 9 digits, classic modulo-97 ---
    fun ukVatBody(rng: Rng, valid: Boolean): String {
        val base = rng.digitsNonZeroLead(7)
        val check = Checksums.ukVatCheckDigits(base)
        if (valid) return base + check
        // Corrupt the two check digits until the modulo-97 rule fails.
        var c = check.toInt()
        var body: String
        do {
            c = (c + 1) % 100
            body = base + c.toString().padStart(2, '0')
        } while (Checksums.isValidUkVat(body))
        return body
    }

    // --- AU ABN: 11 digits, modulo-89 ---
    fun abn(rng: Rng, valid: Boolean): String {
        while (true) {
            val first10 = rng.digitsNonZeroLead(10)
            var partial = 0
            for (i in 0..9) {
                var d = first10[i] - '0'
                if (i == 0) d -= 1
                partial += d * Checksums.ABN_WEIGHTS[i]
            }
            // weight of the 11th digit is 19; 19⁻¹ ≡ 75 (mod 89)
            val r = ((-partial) % 89 + 89) % 89
            val d11 = (75 * r) % 89
            if (d11 > 9) continue
            val abn = first10 + d11
            return if (valid) abn else corruptLastDigit(abn)
        }
    }

    private fun abnFormatted(rng: Rng, valid: Boolean): String {
        val abn = abn(rng, valid)
        // Conventional grouping: 12 345 678 901
        return "${abn.substring(0, 2)} ${abn.substring(2, 5)} ${abn.substring(5, 8)} ${abn.substring(8, 11)}"
    }

    private fun elevenProofNine(rng: Rng): String {
        while (true) {
            val first8 = rng.digitsNonZeroLead(8)
            val weights = intArrayOf(9, 8, 7, 6, 5, 4, 3, 2)
            var sum8 = 0
            for (i in 0..7) sum8 += (first8[i] - '0') * weights[i]
            val d9 = sum8 % 11
            if (d9 == 10) continue
            val nine = first8 + d9
            if (nine != "000000000") return nine
        }
    }

    private fun wrongLetter(letter: Char): Char = if (letter == 'Z') 'A' else letter + 1

    private fun corruptLastDigit(number: String): String {
        val last = number.last() - '0'
        return number.dropLast(1) + ((last + 1) % 10)
    }
}
