package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.JmbgGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JmbgGeneratorTest {

    @Test
    fun `JMBG reference value (documented worked example)`() {
        assertTrue(Checksums.isValidJmbg("0101006500006")) // standard worked example, check digit 6
        assertFalse(Checksums.isValidJmbg("0101006500007")) // wrong check digit
        assertFalse(Checksums.isValidJmbg("010100650000")) // too short
    }

    @Test
    fun `generators round-trip - valid passes, invalid fails, region in range`() {
        val rng = Rng(2024L)
        for (area in JmbgGenerator.Area.entries) {
            repeat(50) {
                val ok = JmbgGenerator.generate(area, rng, valid = true)
                assertTrue(Checksums.isValidJmbg(ok), "expected valid JMBG for ${area.code}: $ok")
                val region = ok.substring(7, 9).toInt()
                assertTrue(region in area.regions, "region $region out of range for ${area.code}")
                assertFalse(Checksums.isValidJmbg(JmbgGenerator.generate(area, rng, valid = false)))
            }
        }
    }
}
