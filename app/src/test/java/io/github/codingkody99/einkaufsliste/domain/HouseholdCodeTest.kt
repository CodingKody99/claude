package io.github.codingkody99.einkaufsliste.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HouseholdCodeTest {

    @Test
    fun `a generated code has the expected shape`() {
        val code = HouseholdCode.generate(Random(1))
        assertEquals(12, code.length)
        assertTrue(HouseholdCode.isValid(code))
    }

    @Test
    fun `generated codes avoid characters people mistype`() {
        val codes = (1..200).map { HouseholdCode.generate(Random(it.toLong())) }
        codes.forEach { code ->
            listOf('I', 'L', 'O', '0', '1').forEach { bad ->
                assertFalse("$code enthält $bad", code.contains(bad))
            }
        }
    }

    @Test
    fun `two codes are not the same`() {
        val codes = (1..500).map { HouseholdCode.generate(Random(it.toLong())) }
        assertEquals(codes.size, codes.distinct().size)
    }

    @Test
    fun `typing a code loosely still works`() {
        val code = HouseholdCode.generate(Random(7))
        val formatted = HouseholdCode.format(code)

        assertEquals(code, HouseholdCode.normalize(formatted))
        assertEquals(code, HouseholdCode.normalize(formatted.lowercase()))
        assertEquals(code, HouseholdCode.normalize(" $formatted "))
        assertEquals(code, HouseholdCode.normalize(code.chunked(3).joinToString(" ")))
    }

    @Test
    fun `a wrong code is rejected rather than guessed at`() {
        assertNull(HouseholdCode.normalize(""))
        assertNull(HouseholdCode.normalize("ABC"))
        assertNull(HouseholdCode.normalize("ABCDEFGHJKMNPQ"))
        // O and I are not in the alphabet, so they cannot be a typo of something valid
        assertNull(HouseholdCode.normalize("ABCDEFGHJKMO"))
        assertNull(HouseholdCode.normalize("ABCDEFGHJKM!"))
    }

    @Test
    fun `the display form is grouped for reading aloud`() {
        assertEquals("ABCD-EFGH-JKMN", HouseholdCode.format("ABCDEFGHJKMN"))
        assertEquals("ABCD-EFGH-JKMN", HouseholdCode.format("abcd efgh jkmn"))
    }

    @Test
    fun `something unparseable is shown as it was given`() {
        assertEquals("kaputt", HouseholdCode.format("kaputt"))
    }
}
