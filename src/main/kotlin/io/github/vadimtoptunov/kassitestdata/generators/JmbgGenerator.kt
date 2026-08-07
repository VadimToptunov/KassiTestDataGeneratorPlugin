package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * JMBG — the ex-Yugoslav 13-digit unique citizen number, still used in Serbia, Slovenia,
 * Montenegro, North Macedonia and Bosnia and Herzegovina. Layout: DDMMYYY (birth date, 3-digit
 * year) + RR (region) + BBB (sequence, also encoding sex) + K (mod-11 check digit). One algorithm,
 * the only per-country difference being the region code range.
 */
object JmbgGenerator {

    enum class Area(val code: String, val displayName: String, val regions: IntRange) {
        BA("BA", "Bosnia and Herzegovina", 10..19),
        ME("ME", "Montenegro", 20..29),
        MK("MK", "North Macedonia", 41..49),
        SI("SI", "Slovenia", 50..59),
        RS("RS", "Serbia", 71..79),
    }

    /** A spec-valid JMBG for [area], or (when [valid] is false) one whose check digit is wrong. */
    fun generate(area: Area, rng: Rng, valid: Boolean): String {
        // Loop because ~1 in 11 (day,month,year,region,sequence) combinations yields the unassignable
        // check value (m == 10); pick a fresh sequence until the combination is assignable.
        while (true) {
            val day = rng.intInRange(1, 28)
            val month = rng.intInRange(1, 12)
            val year = rng.intInRange(1950, 2010)
            val region = rng.intInRange(area.regions.first, area.regions.last)
            val sequence = rng.intInRange(0, 999)
            val first12 = "%02d%02d%03d%02d%03d".format(day, month, year % 1000, region, sequence)
            val check = Checksums.jmbgCheckDigit(first12) ?: continue
            return if (valid) "$first12$check" else "$first12${(check + 1) % 10}"
        }
    }
}
