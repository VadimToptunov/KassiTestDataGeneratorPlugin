package io.github.vadimtoptunov.kassitestdata.core

import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry
import io.github.vadimtoptunov.kassitestdata.data.Names
import io.github.vadimtoptunov.kassitestdata.generators.BankAccountGenerator
import io.github.vadimtoptunov.kassitestdata.generators.BicGenerator
import io.github.vadimtoptunov.kassitestdata.generators.MrzGenerator
import io.github.vadimtoptunov.kassitestdata.generators.NationalIdGenerator
import io.github.vadimtoptunov.kassitestdata.generators.PhoneGenerator
import io.github.vadimtoptunov.kassitestdata.generators.RussianIdGenerator
import io.github.vadimtoptunov.kassitestdata.generators.TaxIdGenerator
import java.time.LocalDate
import java.time.Year

/**
 * A coherent persona: name (consistent with gender/locale), date of birth, national ID,
 * address, and bank + tax identifiers — all for the same country and the same person.
 * KYC verifies a whole identity, not scattered fields; this is that "one smart connective feature".
 * Deterministic under a seed.
 */
data class Persona(
    val country: Country,
    val gender: Gender,
    val fullName: String,
    val dateOfBirth: LocalDate,
    val address: String,
    val bankLabel: String,
    val bankValue: String,
    val bic: String,
    val nationalIdLabel: String?,
    val nationalId: String?,
    val taxLabel: String?,
    val taxId: String?,
    val phone: String?,
    val email: String,
    val passportMrz: String,
    val seed: Long?,
) {
    fun formatted(): String = buildString {
        appendLine("Name:          $fullName")
        appendLine("Gender:        ${if (gender == Gender.MALE) "Male" else "Female"}")
        appendLine("Date of birth: $dateOfBirth")
        appendLine("Country:       ${country.displayName} (${country.code})")
        appendLine("Address:       $address")
        appendLine("Email:${padTo("Email:")}$email")
        appendLine("$bankLabel${padTo(bankLabel)}$bankValue")
        appendLine("BIC:           $bic")
        if (nationalId != null) appendLine("$nationalIdLabel${padTo(nationalIdLabel!!)}$nationalId")
        if (taxId != null) appendLine("$taxLabel${padTo(taxLabel!!)}$taxId")
        if (phone != null) appendLine("Phone:${padTo("Phone:")}$phone")
        appendLine("Passport MRZ:")
        appendLine(passportMrz)
        if (seed != null) append("Seed:          $seed")
    }

    private fun padTo(label: String): String {
        val target = 15
        return " ".repeat((target - label.length).coerceAtLeast(1))
    }
}

object PersonaGenerator {

    private val STREETS = listOf("Main Street", "High Street", "Park Avenue", "Station Road", "Market Square", "Church Lane")
    private val CITIES = listOf("Springfield", "Riverton", "Fairview", "Lakeside", "Newport", "Ashford")

    fun generate(country: Country, seed: Long? = null): Persona {
        val rng = Rng(seed)
        val gender = if (rng.boolean()) Gender.MALE else Gender.FEMALE
        val first = rng.pick(Names.firstName(country.nameLocale, gender))
        val last = rng.pick(Names.lastName(country.nameLocale))

        val age = rng.intInRange(18, 80)
        val dob = LocalDate.of(Year.now().value - age, rng.intInRange(1, 12), rng.intInRange(1, 28))

        val address = "${rng.intInRange(1, 200)} ${rng.pick(STREETS)}, " +
            "${rng.digits(if (country == Country.GB) 4 else 5)} ${rng.pick(CITIES)}, ${country.code}"

        val isIban = IbanRegistry.specFor(country) != null
        val bankLabel: String
        val bankValue: String
        when {
            // Russia has an IBAN (national standard) but domestically uses БИК + account, which is the
            // realistic identity for a persona — so this is checked before the generic IBAN branch.
            country == Country.RU -> {
                val bik = RussianIdGenerator.bik(rng)
                bankLabel = "Bank (БИК+счёт):"
                bankValue = "БИК $bik · Счёт ${RussianIdGenerator.account(rng, bik)}"
            }
            isIban -> {
                bankLabel = "IBAN:"
                bankValue = BankAccountGenerator.ibanFormatted(country, rng, valid = true)
            }
            country == Country.AU -> {
                bankLabel = "Bank (BSB):"
                bankValue = BankAccountGenerator.auBsbAndAccount(rng)
            }
            else -> {
                bankLabel = "Bank:"
                bankValue = "-"
            }
        }

        val bic = BicGenerator.bic(country, rng)

        val nationalScheme = NationalIdGenerator.supported[country]
        val nationalId = nationalScheme?.let {
            NationalIdGenerator.generate(
                country, rng, valid = true, birth = dob, gender = gender, surname = last, givenName = first,
            )
        }
        val nationalLabel = nationalScheme?.let { "${it.label}:" }

        val taxScheme = TaxIdGenerator.supported[country]
        val taxId = taxScheme?.let { TaxIdGenerator.generate(country, rng, valid = true) }
        val taxLabel = taxScheme?.let { "${it.label}:" }

        val phone = if (PhoneGenerator.isSupported(country)) PhoneGenerator.generate(country, rng, valid = true) else null

        // Email coherent with the name (RFC 2606 reserved domain), e.g. anna.eriksson@example.com.
        val email = coherentEmail(first, last, rng)

        // Passport MRZ (ICAO TD3), coherent with the persona's name, nationality, DOB and sex.
        val passportMrz = MrzGenerator.td3(
            country, rng, valid = true, birth = dob, gender = gender, surname = last, givenNames = first,
        )

        return Persona(
            country = country,
            gender = gender,
            fullName = "$first $last",
            dateOfBirth = dob,
            address = address,
            bankLabel = bankLabel,
            bankValue = bankValue,
            bic = bic,
            nationalIdLabel = nationalLabel,
            nationalId = nationalId,
            taxLabel = taxLabel,
            taxId = taxId,
            phone = phone,
            email = email,
            passportMrz = passportMrz,
            seed = seed,
        )
    }

    /** An address-safe local part derived from the persona's name: strip diacritics, keep [a-z0-9]. */
    private fun coherentEmail(first: String, last: String, rng: Rng): String {
        fun slug(s: String): String = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("[^A-Za-z0-9]"), "").lowercase()
        val f = slug(first)
        val l = slug(last)
        val local = if (f.isEmpty() || l.isEmpty()) "user${rng.digits(3)}" else "$f.$l"
        return "$local@example.com"
    }
}
