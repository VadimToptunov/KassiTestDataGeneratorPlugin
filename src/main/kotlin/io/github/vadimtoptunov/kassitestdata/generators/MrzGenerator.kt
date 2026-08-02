package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Gender
import io.github.vadimtoptunov.kassitestdata.core.Rng
import java.time.LocalDate

/**
 * Passport Machine Readable Zone (ICAO 9303, TD3 — two 44-character lines). Universal: the MRZ check
 * digits are computable for any country, so this works for every ISO country code. Each generated MRZ
 * carries valid field check digits and a valid composite check digit, so it passes an MRZ validator
 * (the same 7-3-1 routine the MRZ Inspector plugin uses to parse them).
 */
object MrzGenerator {

    private const val LINE_LENGTH = 44
    private val WEIGHTS = intArrayOf(7, 3, 1)

    // ISO 3166-1 alpha-3 codes for the covered countries (MRZ uses the 3-letter code).
    private val ALPHA3 = mapOf(
        "AD" to "AND", "AL" to "ALB", "AT" to "AUT", "BA" to "BIH", "BE" to "BEL", "BG" to "BGR",
        "CH" to "CHE", "CY" to "CYP", "CZ" to "CZE", "DE" to "DEU", "DK" to "DNK", "EE" to "EST",
        "ES" to "ESP", "FI" to "FIN", "FO" to "FRO", "FR" to "FRA", "GB" to "GBR", "GI" to "GIB",
        "GL" to "GRL", "GR" to "GRC", "HR" to "HRV", "HU" to "HUN", "IE" to "IRL", "IS" to "ISL",
        "IT" to "ITA", "LI" to "LIE", "LT" to "LTU", "LU" to "LUX", "LV" to "LVA", "MC" to "MCO",
        "MD" to "MDA", "ME" to "MNE", "MK" to "MKD", "MT" to "MLT", "NL" to "NLD", "NO" to "NOR",
        "PL" to "POL", "PT" to "PRT", "RO" to "ROU", "RS" to "SRB", "SE" to "SWE", "SI" to "SVN",
        "SK" to "SVK", "SM" to "SMR", "UA" to "UKR", "VA" to "VAT", "XK" to "RKS", "AU" to "AUS",
        "AE" to "ARE", "AZ" to "AZE", "BH" to "BHR", "BI" to "BDI", "BR" to "BRA", "BY" to "BLR",
        "CR" to "CRI", "DJ" to "DJI", "DO" to "DOM", "EG" to "EGY", "FK" to "FLK", "GE" to "GEO",
        "GT" to "GTM", "HN" to "HND", "IL" to "ISR", "IQ" to "IRQ", "JO" to "JOR", "KW" to "KWT",
        "KZ" to "KAZ", "LB" to "LBN", "LC" to "LCA", "LY" to "LBY", "MN" to "MNG", "MR" to "MRT",
        "MU" to "MUS", "NI" to "NIC", "OM" to "OMN", "PK" to "PAK", "PS" to "PSE", "QA" to "QAT",
        "SA" to "SAU", "SC" to "SYC", "SD" to "SDN", "SO" to "SOM", "ST" to "STP", "SV" to "SLV",
        "TL" to "TLS", "TN" to "TUN", "TR" to "TUR", "VG" to "VGB", "YE" to "YEM",
    )

    fun alpha3(country: Country): String = ALPHA3[country.code] ?: country.code.padEnd(3, 'X')

    private fun charValue(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'A'..'Z' -> c - 'A' + 10
        else -> 0 // '<' and any filler
    }

    /** ICAO 9303 check digit (weights 7,3,1). */
    fun checkDigit(input: String): Int {
        var sum = 0
        for (i in input.indices) sum += charValue(input[i]) * WEIGHTS[i % 3]
        return sum % 10
    }

    private fun yymmdd(date: LocalDate): String =
        (date.year % 100).toString().padStart(2, '0') +
            date.monthValue.toString().padStart(2, '0') +
            date.dayOfMonth.toString().padStart(2, '0')

    /** The 39-character name field: PRIMARY&lt;&lt;SECONDARY, non-letters as filler, padded with '<'. */
    private fun nameField(surname: String, givenNames: String): String {
        fun clean(s: String) = s.uppercase().map { if (it in 'A'..'Z') it else '<' }
            .joinToString("").replace(Regex("<+"), "<").trim('<')
        val field = "${clean(surname)}<<${clean(givenNames)}"
        return (field + "<".repeat(LINE_LENGTH)).substring(0, 39)
    }

    /** A passport TD3 MRZ (two lines joined by a newline). [valid]=false corrupts the composite check digit. */
    fun td3(
        country: Country,
        rng: Rng,
        valid: Boolean = true,
        birth: LocalDate? = null,
        gender: Gender? = null,
        surname: String? = null,
        givenNames: String? = null,
    ): String {
        val a3 = alpha3(country)
        val sex = gender ?: if (rng.boolean()) Gender.MALE else Gender.FEMALE
        val dob = birth ?: LocalDate.of(rng.intInRange(1950, 2005), rng.intInRange(1, 12), rng.intInRange(1, 28))
        val sn = surname ?: rng.upperLetters(rng.intInRange(4, 8))
        val gn = givenNames ?: rng.upperLetters(rng.intInRange(4, 7))

        val line1 = "P<$a3${nameField(sn, gn)}"

        val passportNo = rng.alnums(9)
        val passportChk = checkDigit(passportNo)
        val dobStr = yymmdd(dob)
        val dobChk = checkDigit(dobStr)
        val sexChar = if (sex == Gender.MALE) 'M' else 'F'
        val expiry = rng.intInRange(26, 35).toString().padStart(2, '0') +
            rng.intInRange(1, 12).toString().padStart(2, '0') +
            rng.intInRange(1, 28).toString().padStart(2, '0')
        val expiryChk = checkDigit(expiry)
        val personal = "<".repeat(14)
        val personalChk = checkDigit(personal) // 0 for an all-filler optional-data field

        val composite = "$passportNo$passportChk$dobStr$dobChk$expiry$expiryChk$personal$personalChk"
        val compositeChk = if (valid) checkDigit(composite) else (checkDigit(composite) + 1) % 10

        val line2 = "$passportNo$passportChk$a3$dobStr$dobChk$sexChar$expiry$expiryChk$personal$personalChk$compositeChk"
        return "$line1\n$line2"
    }

    /** Validate a TD3 MRZ: two 44-char lines and every field + composite check digit correct. */
    fun isValidTd3(mrz: String): Boolean {
        val lines = mrz.split("\n")
        if (lines.size != 2 || lines[0].length != LINE_LENGTH || lines[1].length != LINE_LENGTH) return false
        val l2 = lines[1]
        fun digitAt(i: Int) = if (l2[i] in '0'..'9') l2[i] - '0' else -1
        if (checkDigit(l2.substring(0, 9)) != digitAt(9)) return false
        if (checkDigit(l2.substring(13, 19)) != digitAt(19)) return false
        if (checkDigit(l2.substring(21, 27)) != digitAt(27)) return false
        if (checkDigit(l2.substring(28, 42)) != digitAt(42)) return false
        val composite = l2.substring(0, 10) + l2.substring(13, 20) + l2.substring(21, 43)
        return checkDigit(composite) == digitAt(43)
    }
}
