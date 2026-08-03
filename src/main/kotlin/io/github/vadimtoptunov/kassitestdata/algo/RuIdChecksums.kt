package io.github.vadimtoptunov.kassitestdata.algo

/**
 * Checksums for Russian identifiers — ИНН (individual & legal), СНИЛС, ОГРН, ОГРНИП. Each is an
 * independent implementation of the published national algorithm; generators produce values that
 * satisfy these, and unit tests anchor them to known reference values.
 */
object RuIdChecksums {

    // ---- ИНН — legal entity (10 digits, weighted mod 11) ----
    private val INN10_W = intArrayOf(2, 4, 10, 3, 5, 9, 4, 6, 8)

    fun innLegalCheck(first9: String): Int {
        var sum = 0
        for (i in 0..8) sum += (first9[i] - '0') * INN10_W[i]
        return (sum % 11) % 10
    }

    fun isValidInnLegal(value: String): Boolean {
        if (value.length != 10 || !value.all { it in '0'..'9' }) return false
        return innLegalCheck(value.substring(0, 9)) == (value[9] - '0')
    }

    // ---- ИНН — individual (12 digits, two weighted mod-11 check digits) ----
    private val INN12_W11 = intArrayOf(7, 2, 4, 10, 3, 5, 9, 4, 6, 8)
    private val INN12_W12 = intArrayOf(3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8)

    fun innIndividualCheck11(first10: String): Int {
        var sum = 0
        for (i in 0..9) sum += (first10[i] - '0') * INN12_W11[i]
        return (sum % 11) % 10
    }

    fun innIndividualCheck12(first11: String): Int {
        var sum = 0
        for (i in 0..10) sum += (first11[i] - '0') * INN12_W12[i]
        return (sum % 11) % 10
    }

    fun isValidInnIndividual(value: String): Boolean {
        if (value.length != 12 || !value.all { it in '0'..'9' }) return false
        return innIndividualCheck11(value.substring(0, 10)) == (value[10] - '0') &&
            innIndividualCheck12(value.substring(0, 11)) == (value[11] - '0')
    }

    // ---- СНИЛС (11 digits: 9 + a 2-digit check, weighted mod 101) ----
    fun snilsCheck(first9: String): Int {
        var sum = 0
        for (i in 0..8) sum += (first9[i] - '0') * (9 - i)
        val c = sum % 101
        return if (c == 100) 0 else c // 100 and 101 both record as 00
    }

    fun isValidSnils(value: String): Boolean {
        if (value.length != 11 || !value.all { it in '0'..'9' }) return false
        return snilsCheck(value.substring(0, 9)) == value.substring(9).toInt()
    }

    // ---- ОГРН (13 digits; check = (first 12 mod 11) mod 10) ----
    fun ogrnCheck(first12: String): Int = ((first12.toLong() % 11) % 10).toInt()

    fun isValidOgrn(value: String): Boolean {
        if (value.length != 13 || !value.all { it in '0'..'9' }) return false
        return ogrnCheck(value.substring(0, 12)) == (value[12] - '0')
    }

    // ---- Bank account "ключевание" (20-digit account keyed against a 9-digit БИК) ----
    private val ACCOUNT_WEIGHTS = intArrayOf(7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1)

    /** The 3-digit prefix prepended to the account before keying: for correspondent accounts (30xxx)
     *  it is `0` + БИК digits 5–6; otherwise the last 3 digits of the БИК. */
    private fun accountPrefix(bik: String, account: String): String =
        if (account.startsWith("301") || account.startsWith("303")) "0" + bik.substring(4, 6)
        else bik.substring(6, 9)

    /** Weighted sum mod 10 of (prefix + account); 0 means the account keys correctly to the БИК. */
    fun accountKeySum(bik: String, account: String): Int {
        val s = accountPrefix(bik, account) + account
        var sum = 0
        for (i in 0..22) sum += (s[i] - '0') * ACCOUNT_WEIGHTS[i]
        return sum % 10
    }

    fun isValidRussianAccount(bik: String, account: String): Boolean {
        if (bik.length != 9 || account.length != 20) return false
        if (!bik.all { it in '0'..'9' } || !account.all { it in '0'..'9' }) return false
        return accountKeySum(bik, account) == 0
    }

    // ---- ОГРНИП (15 digits; check = (first 14 mod 13) mod 10) ----
    fun ogrnipCheck(first14: String): Int = ((first14.toLong() % 13) % 10).toInt()

    fun isValidOgrnip(value: String): Boolean {
        if (value.length != 15 || !value.all { it in '0'..'9' }) return false
        return ogrnipCheck(value.substring(0, 14)) == (value[14] - '0')
    }
}
