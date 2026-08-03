package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.RuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * Russian identifiers with real checksums: ИНН (individual & legal), СНИЛС, ОГРН, ОГРНИП.
 * Each returns a spec-valid value or (when [valid] is false) one that fails its checksum.
 */
object RussianIdGenerator {

    /** СНИЛС — 11 digits (used as the personal "national ID"). */
    fun snils(rng: Rng, valid: Boolean): String {
        val first9 = rng.digitsNonZeroLead(9)
        val check = RuIdChecksums.snilsCheck(first9)
        val checkStr = check.toString().padStart(2, '0')
        return if (valid) first9 + checkStr else first9 + ((check + 1) % 100).toString().padStart(2, '0')
    }

    /** ИНН физического лица — 12 digits (used as the personal tax ID). */
    fun innIndividual(rng: Rng, valid: Boolean): String {
        val first10 = rng.digitsNonZeroLead(10)
        val first11 = first10 + RuIdChecksums.innIndividualCheck11(first10)
        val check12 = RuIdChecksums.innIndividualCheck12(first11)
        return if (valid) "$first11$check12" else first11 + ((check12 + 1) % 10)
    }

    /** ИНН юридического лица — 10 digits. */
    fun innLegal(rng: Rng, valid: Boolean): String {
        val first9 = rng.digitsNonZeroLead(9)
        val check = RuIdChecksums.innLegalCheck(first9)
        return if (valid) "$first9$check" else first9 + ((check + 1) % 10)
    }

    /** ОГРН — 13 digits (main state registration number of a legal entity). */
    fun ogrn(rng: Rng, valid: Boolean): String {
        val first12 = "1" + rng.digits(11) // ОГРН leads with 1/2/3/5; use 1
        val check = RuIdChecksums.ogrnCheck(first12)
        return if (valid) "$first12$check" else first12 + ((check + 1) % 10)
    }

    /** ОГРНИП — 15 digits (state registration number of an individual entrepreneur). */
    fun ogrnip(rng: Rng, valid: Boolean): String {
        val first14 = "3" + rng.digits(13) // ОГРНИП leads with 3/4; use 3
        val check = RuIdChecksums.ogrnipCheck(first14)
        return if (valid) "$first14$check" else first14 + ((check + 1) % 10)
    }
}
