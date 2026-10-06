package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Anchors each algorithm to published reference values, so the checksum code is verified
 * against the real world and not just against itself.
 */
class ChecksumsTest {

    @Test
    fun `IBAN mod-97 accepts known-valid and rejects corrupted`() {
        assertTrue(Checksums.isValidIbanMod97("GB82 WEST 1234 5698 7654 32"))
        assertTrue(Checksums.isValidIbanMod97("DE89370400440532013000"))
        assertTrue(Checksums.isValidIbanMod97("NL91ABNA0417164300"))
        assertFalse(Checksums.isValidIbanMod97("GB82WEST12345698765433")) // last digit changed
        assertFalse(Checksums.isValidIbanMod97("DE90370400440532013000")) // check digits changed
    }

    @Test
    fun `Luhn accepts known test PANs and rejects off-by-one`() {
        assertTrue(Checksums.isLuhnValid("4242424242424242"))
        assertTrue(Checksums.isLuhnValid("378282246310005"))
        assertFalse(Checksums.isLuhnValid("4242424242424241"))
    }

    @Test
    fun `Dutch 11-proef reference`() {
        assertTrue(Checksums.isValidElevenProof("123456782"))
        assertFalse(Checksums.isValidElevenProof("123456789"))
        assertFalse(Checksums.isValidElevenProof("000000000"))
    }

    @Test
    fun `Australian TFN reference`() {
        assertTrue(Checksums.isValidTfn("123456782"))
        assertFalse(Checksums.isValidTfn("123456781"))
    }

    @Test
    fun `Australian ABN reference (ABR sample)`() {
        assertTrue(Checksums.isValidAbn("51824753556"))
        assertFalse(Checksums.isValidAbn("51824753557"))
    }

    @Test
    fun `German VAT (USt-IdNr) reference`() {
        assertTrue(Checksums.isValidGermanVat("136695976"))
        assertFalse(Checksums.isValidGermanVat("136695975"))
        assertEquals(6, Checksums.mod1110CheckDigit("13669597"))
    }

    @Test
    fun `UK VAT modulo-97 reference`() {
        assertTrue(Checksums.isValidUkVat("980780684"))
        assertFalse(Checksums.isValidUkVat("980780685"))
    }

    @Test
    fun `Cyprus VAT check-letter reference`() {
        assertEquals('P', Checksums.cyprusVatCheckLetter("10259033"))
        assertTrue(Checksums.isValidCyprusVat("10259033P"))
        assertFalse(Checksums.isValidCyprusVat("10259033Q"))
    }

    @Test
    fun `CUSIP check digit reference (real securities)`() {
        // Published CUSIPs from Wikipedia's worked list (https://en.wikipedia.org/wiki/CUSIP):
        // Apple Inc. 037833100, Microsoft Corporation 594918104, Cisco Systems 17275R102 (letter in base).
        assertEquals(0, Checksums.cusipCheckDigit("03783310")) // Apple → 037833100
        assertTrue(Checksums.isValidCusip("037833100"))
        assertTrue(Checksums.isValidCusip("594918104")) // Microsoft
        assertTrue(Checksums.isValidCusip("17275R102")) // Cisco — 'R' in the base
        assertFalse(Checksums.isValidCusip("037833101")) // check digit off by one
    }

    @Test
    fun `SEDOL check digit reference (published examples)`() {
        // External worked examples from Wikipedia (https://en.wikipedia.org/wiki/SEDOL):
        // B0YBKJ7 (base B0YBKJ, weighted sum 353 → 7) and the numeric 0263494 and 0540528.
        assertEquals(7, Checksums.sedolCheckDigit("B0YBKJ"))
        assertTrue(Checksums.isValidSedol("B0YBKJ7"))
        assertTrue(Checksums.isValidSedol("0263494"))
        assertTrue(Checksums.isValidSedol("0540528"))
        assertFalse(Checksums.isValidSedol("B0YBKJ8")) // check digit off by one
        assertFalse(Checksums.isValidSedol("A0YBKJ7")) // vowel 'A' is not a legal SEDOL character
    }

    @Test
    fun `ABA routing check digit reference (real Federal Reserve routing numbers)`() {
        // External anchor: real, publicly published U.S. routing transit numbers, all known-valid —
        // JPMorgan Chase, FRB Boston, Wells Fargo, Bank of America (VA), Citibank (NY).
        for (routing in listOf("021000021", "011000015", "121000248", "051000017", "021000089")) {
            assertTrue(Checksums.isValidAbaRouting(routing), "$routing should be valid")
            assertEquals(routing[8] - '0', Checksums.abaRoutingCheckDigit(routing.substring(0, 8)))
        }
        assertFalse(Checksums.isValidAbaRouting("021000022")) // check digit off by one
        assertFalse(Checksums.isValidAbaRouting("12345678"))  // too short
    }

    @Test
    fun `ISSN check digit reference (published serials)`() {
        // External anchor: real published ISSNs — Nature (0028-0836) and the Wikipedia worked
        // example (0317-8471, check digit 1).
        assertTrue(Checksums.isValidIssn("0028-0836"))
        assertTrue(Checksums.isValidIssn("0317-8471"))
        assertEquals('1', Checksums.issnCheckChar("0317847"))
        assertFalse(Checksums.isValidIssn("0028-0837")) // check digit off by one
    }

    @Test
    fun `ICAO 7-3-1 check digit reference`() {
        // Classic ICAO 9303 passport-number example: "L898902C" → check digit 3.
        assertEquals(3, Checksums.icao731CheckDigit("L898902C"))
    }
}
