# Publishing to the JetBrains Marketplace

A one-time-per-release checklist to take this from source to a listed plugin.

## 0. Prerequisites (once)

1. Sign in at <https://plugins.jetbrains.com> with a JetBrains account.
2. Accept the Marketplace developer agreement.
3. Create a **Personal Access Token**: Marketplace ▸ your profile ▸ **My Tokens** → generate. This is your `PUBLISH_TOKEN`.

## 1. Build the distribution

```bash
./gradlew clean test buildPlugin
```

Output: `build/distributions/Kassi-Test-Data-1.0.0.zip`. Verify it:

```bash
./gradlew verifyPlugin        # structural checks
./gradlew runPluginVerifier   # (optional) compatibility against a range of IDEs
```

## 2. First release — upload manually

The **first** version of a new plugin must be uploaded through the web UI so a human can approve it:

1. Go to <https://plugins.jetbrains.com/plugin/add>.
2. Upload `build/distributions/Kassi-Test-Data-1.0.0.zip`.
3. Category: **Tools / Code tools**. Fill in the description (already embedded from `plugin.xml`), tags
   (`test data`, `qa`, `fintech`, `kyc`, `iban`, `generator`), and a source/repo URL.
4. Submit. Moderation typically takes **1–3 business days**. You'll get an email when it's listed.

## 3. Subsequent releases — automated

After the plugin exists and you have a token, later versions can publish from the CLI:

```bash
export PUBLISH_TOKEN=xxxxxxxx
./gradlew publishPlugin
```

To publish to a non-default channel (e.g. an early-access `beta` channel), configure
`publishPlugin { channels.set(listOf("beta")) }` in `build.gradle.kts`.

### Optional: signing

The Marketplace can verify a signed plugin. Generate a certificate chain + private key and set:

```bash
export CERTIFICATE_CHAIN=...   # PEM
export PRIVATE_KEY=...         # PEM
export PRIVATE_KEY_PASSWORD=...
./gradlew signPlugin publishPlugin
```

See <https://plugins.jetbrains.com/docs/intellij/plugin-signing.html>.

## 4. Version bump for the next release

1. Update `version` in `build.gradle.kts`.
2. Add a `<change-notes>` entry in `plugin.xml`.
3. Re-run step 1 and step 3.

Per the roadmap, **one version is fully listed before the next begins**.
