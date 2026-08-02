package io.github.vadimtoptunov.kassitestdata.core

/**
 * Coverage target: all of Europe (EU-27 + EEA + UK + Switzerland + IBAN micro-states)
 * plus Cyprus (called out as home market) and Australia (the deliberate non-European case).
 *
 * [european] drives UI grouping only. [nameLocale] selects the persona name pool; several
 * countries share a locale, and anything without a dedicated pool falls back to a neutral set.
 */
enum class Country(
    val code: String,
    val displayName: String,
    val european: Boolean,
    val nameLocale: NameLocale,
) {
    AD("AD", "Andorra", true, NameLocale.ES),
    AL("AL", "Albania", true, NameLocale.NEUTRAL),
    AT("AT", "Austria", true, NameLocale.DE),
    BA("BA", "Bosnia and Herzegovina", true, NameLocale.NEUTRAL),
    BE("BE", "Belgium", true, NameLocale.NL),
    BG("BG", "Bulgaria", true, NameLocale.NEUTRAL),
    CH("CH", "Switzerland", true, NameLocale.DE),
    CY("CY", "Cyprus", true, NameLocale.NEUTRAL),
    CZ("CZ", "Czechia", true, NameLocale.NEUTRAL),
    DE("DE", "Germany", true, NameLocale.DE),
    DK("DK", "Denmark", true, NameLocale.NEUTRAL),
    EE("EE", "Estonia", true, NameLocale.NEUTRAL),
    ES("ES", "Spain", true, NameLocale.ES),
    FI("FI", "Finland", true, NameLocale.NEUTRAL),
    FO("FO", "Faroe Islands", true, NameLocale.NEUTRAL),
    FR("FR", "France", true, NameLocale.FR),
    GB("GB", "United Kingdom", true, NameLocale.EN),
    GI("GI", "Gibraltar", true, NameLocale.EN),
    GL("GL", "Greenland", true, NameLocale.NEUTRAL),
    GR("GR", "Greece", true, NameLocale.NEUTRAL),
    HR("HR", "Croatia", true, NameLocale.NEUTRAL),
    HU("HU", "Hungary", true, NameLocale.NEUTRAL),
    IE("IE", "Ireland", true, NameLocale.EN),
    IS("IS", "Iceland", true, NameLocale.NEUTRAL),
    IT("IT", "Italy", true, NameLocale.IT),
    LI("LI", "Liechtenstein", true, NameLocale.DE),
    LT("LT", "Lithuania", true, NameLocale.NEUTRAL),
    LU("LU", "Luxembourg", true, NameLocale.FR),
    LV("LV", "Latvia", true, NameLocale.NEUTRAL),
    MC("MC", "Monaco", true, NameLocale.FR),
    MD("MD", "Moldova", true, NameLocale.NEUTRAL),
    ME("ME", "Montenegro", true, NameLocale.NEUTRAL),
    MK("MK", "North Macedonia", true, NameLocale.NEUTRAL),
    MT("MT", "Malta", true, NameLocale.EN),
    NL("NL", "Netherlands", true, NameLocale.NL),
    NO("NO", "Norway", true, NameLocale.NEUTRAL),
    PL("PL", "Poland", true, NameLocale.NEUTRAL),
    PT("PT", "Portugal", true, NameLocale.PT),
    RO("RO", "Romania", true, NameLocale.NEUTRAL),
    RS("RS", "Serbia", true, NameLocale.NEUTRAL),
    SE("SE", "Sweden", true, NameLocale.NEUTRAL),
    SI("SI", "Slovenia", true, NameLocale.NEUTRAL),
    SK("SK", "Slovakia", true, NameLocale.NEUTRAL),
    SM("SM", "San Marino", true, NameLocale.IT),
    UA("UA", "Ukraine", true, NameLocale.NEUTRAL),
    VA("VA", "Vatican City", true, NameLocale.IT),
    XK("XK", "Kosovo", true, NameLocale.NEUTRAL),

    // Worldwide IBAN registry (ISO 13616) beyond the original European set. IBAN + BIC + persona;
    // no national-ID / VAT checksum unless a real one is added later. Russia is intentionally excluded
    // (disputed status in the official SWIFT IBAN registry).
    AE("AE", "United Arab Emirates", false, NameLocale.NEUTRAL),
    AZ("AZ", "Azerbaijan", false, NameLocale.NEUTRAL),
    BH("BH", "Bahrain", false, NameLocale.NEUTRAL),
    BI("BI", "Burundi", false, NameLocale.NEUTRAL),
    BR("BR", "Brazil", false, NameLocale.PT),
    BY("BY", "Belarus", false, NameLocale.NEUTRAL),
    CR("CR", "Costa Rica", false, NameLocale.ES),
    DJ("DJ", "Djibouti", false, NameLocale.FR),
    DO("DO", "Dominican Republic", false, NameLocale.ES),
    EG("EG", "Egypt", false, NameLocale.NEUTRAL),
    FK("FK", "Falkland Islands", false, NameLocale.EN),
    GE("GE", "Georgia", false, NameLocale.NEUTRAL),
    GT("GT", "Guatemala", false, NameLocale.ES),
    HN("HN", "Honduras", false, NameLocale.ES),
    IL("IL", "Israel", false, NameLocale.NEUTRAL),
    IQ("IQ", "Iraq", false, NameLocale.NEUTRAL),
    JO("JO", "Jordan", false, NameLocale.NEUTRAL),
    KW("KW", "Kuwait", false, NameLocale.NEUTRAL),
    KZ("KZ", "Kazakhstan", false, NameLocale.NEUTRAL),
    LB("LB", "Lebanon", false, NameLocale.FR),
    LC("LC", "Saint Lucia", false, NameLocale.EN),
    LY("LY", "Libya", false, NameLocale.NEUTRAL),
    MN("MN", "Mongolia", false, NameLocale.NEUTRAL),
    MR("MR", "Mauritania", false, NameLocale.FR),
    MU("MU", "Mauritius", false, NameLocale.EN),
    NI("NI", "Nicaragua", false, NameLocale.ES),
    OM("OM", "Oman", false, NameLocale.NEUTRAL),
    PK("PK", "Pakistan", false, NameLocale.EN),
    PS("PS", "Palestine", false, NameLocale.NEUTRAL),
    QA("QA", "Qatar", false, NameLocale.NEUTRAL),
    SA("SA", "Saudi Arabia", false, NameLocale.NEUTRAL),
    SC("SC", "Seychelles", false, NameLocale.EN),
    SD("SD", "Sudan", false, NameLocale.NEUTRAL),
    SO("SO", "Somalia", false, NameLocale.NEUTRAL),
    ST("ST", "São Tomé and Príncipe", false, NameLocale.PT),
    SV("SV", "El Salvador", false, NameLocale.ES),
    TL("TL", "Timor-Leste", false, NameLocale.PT),
    TN("TN", "Tunisia", false, NameLocale.FR),
    TR("TR", "Türkiye", false, NameLocale.NEUTRAL),
    VG("VG", "British Virgin Islands", false, NameLocale.EN),
    YE("YE", "Yemen", false, NameLocale.NEUTRAL),

    // Non-European case that forces a clean per-country abstraction (no IBAN, no VAT).
    AU("AU", "Australia", false, NameLocale.EN),
    ;

    companion object {
        fun fromCode(code: String): Country? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

enum class NameLocale { EN, DE, NL, FR, IT, ES, PT, NEUTRAL }

enum class Gender { MALE, FEMALE }
