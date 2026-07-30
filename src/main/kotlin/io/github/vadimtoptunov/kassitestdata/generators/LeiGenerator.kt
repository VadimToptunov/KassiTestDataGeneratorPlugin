package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.EuIdChecksums
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * LEI — Legal Entity Identifier (ISO 17442): 20 chars = 4-char LOU prefix + 2 reserved "00" +
 * 12-char entity part + 2 ISO 7064 MOD 97-10 check digits. Country-agnostic business identifier
 * for KYB flows. Never a real registered entity — synthetic and structurally valid only.
 */
object LeiGenerator {

    fun generate(rng: Rng, valid: Boolean = true): String {
        val base18 = rng.alnums(4) + "00" + rng.alnums(12)
        val check = EuIdChecksums.leiCheckDigits(base18)
        if (valid) return base18 + check
        var c = check.toInt()
        var lei: String
        do {
            c = (c + 1) % 100
            lei = base18 + c.toString().padStart(2, '0')
        } while (EuIdChecksums.isValidLei(lei))
        return lei
    }

    fun isValid(lei: String): Boolean = EuIdChecksums.isValidLei(lei)
}
