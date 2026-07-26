# Kassi Test Data

[![Build](https://github.com/VadimToptunov/KassiTestDataGeneratorPlugin/actions/workflows/build.yml/badge.svg)](https://github.com/VadimToptunov/KassiTestDataGeneratorPlugin/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
<!-- JetBrains Marketplace badges — after the first listing, replace <ID> with the numeric plugin id
     from the plugin's Marketplace URL (plugins.jetbrains.com/plugin/<ID>-...), then uncomment:
[![Version](https://img.shields.io/jetbrains/plugin/v/<ID>.svg?label=Marketplace)](https://plugins.jetbrains.com/plugin/<ID>)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/<ID>.svg)](https://plugins.jetbrains.com/plugin/<ID>)
-->

A JetBrains / Android Studio plugin that generates **spec-valid** banking and KYC test data —
data that actually passes a real validator — plus the deliberately **invalid** variants you need
to test rejection paths. Offline, deterministic, no server, no real PII.

Generic faker tools (including the built-in "Test Data" plugin) produce *plausible-looking* data:
names, emails, random numbers. They do not produce data that passes `mod-97`, Luhn, or a national
checksum. This plugin does — that is the whole point, and every generator is backed by a unit test
that proves it.

## What it generates

| Generator | Coverage | Validity |
|-----------|----------|----------|
| **IBAN** | All European IBAN countries | ISO 13616 length + ISO 7064 mod-97, valid **and** invalid variant |
| **Card (PAN)** | Visa / Mastercard / Amex | Luhn-valid from **test BIN ranges only**, + CVV + expiry, valid & invalid |
| **BIC/SWIFT** | All countries | ISO 9362 structure, country-consistent |
| **National ID** | NL BSN · DE Personalausweis · AU TFN (checksum); GB NINo · CY (format) | Real per-scheme check |
| **Tax / VAT** | CY · DE · NL · GB VAT; AU ABN | Real per-scheme checksum |
| **Persona** | All countries | Coherent bundle: name, DOB, address, bank + tax ID for one person, seedable |

> **Breadth note:** IBAN, BIC, Card and Persona cover **all European countries + Australia** today,
> because their algorithms generalize. National-ID and VAT checksums ship for CY/DE/NL/GB/AU in v1
> and expand per country over subsequent versions (see the roadmap). The UI only offers a
> (country × generator) combination when it is backed by a real, tested check.

## Usage

- Right-click in the editor → **Insert Test Data…** (also under **Tools** and **Alt+Insert** / Generate).
- A searchable popup lists every generator × country × variant. Type to filter (`iban de`, `persona`, `card amex`).
- The selection is inserted at the caret.
- **Settings ▸ Tools ▸ Kassi Test Data** → set an optional numeric **seed** for reproducible output.

## Safety & compliance

- **Cards:** Luhn-valid numbers built from published payment-gateway **test** BINs only — never real issuable PANs.
- **IDs:** structurally valid but **synthetic**. Zero real PII, zero scraping — purely algorithmic and offline.
- **Permanent boundary:** the plugin generates *data* (fields, checksums), never a library of document images/specimens.

## Correctness discipline (the moat)

Each generator has its own unit test asserting the valid output **passes** the real algorithm and the
invalid variant **fails** it. Checksum routines are additionally anchored to published reference values
(e.g. the `GB82 WEST…` IBAN, the `4242…` test card, the ABR sample ABN `51 824 753 556`). IBAN output is
verified in tests by an *independent* `BigInteger` mod-97 implementation, so the check isn't circular.

```bash
./gradlew test          # run the full validity test suite
./gradlew buildPlugin   # produce build/distributions/*.zip for the Marketplace
./gradlew runIde        # try it in a sandbox IDE
```

## Building & compatibility

- Built with the [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/) (1.17.x).
- Platform-only (`com.intellij.modules.platform`) → loads in IntelliJ IDEA, Android Studio, and other IntelliJ-based IDEs.
- `sinceBuild = 232` (2023.2+).

See [PUBLISHING.md](PUBLISHING.md) for the Marketplace submission steps.

## License

MIT — see [LICENSE](LICENSE).
