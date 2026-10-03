package io.github.codingkody99.einkaufsliste.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantityParserTest {

    private fun assertSplit(input: String, quantity: String, name: String) =
        assertEquals("\"$input\"", quantity to name, QuantityParser.split(input))

    @Test
    fun `amount in front with a unit`() {
        assertSplit("500g Hackfleisch", "500 g", "Hackfleisch")
        assertSplit("500 g Hackfleisch", "500 g", "Hackfleisch")
        assertSplit("1 kg Kartoffeln", "1 kg", "Kartoffeln")
        assertSplit("0,5 l Sahne", "0,5 l", "Sahne")
        assertSplit("1 Packung Butter", "1 Packung", "Butter")
        assertSplit("2 Dosen Tomaten", "2 Dose", "Tomaten")
    }

    @Test
    fun `bare number in front stays an amount`() {
        assertSplit("2 Joghurt", "2", "Joghurt")
        assertSplit("3 Zitronen", "3", "Zitronen")
    }

    @Test
    fun `a word after the number is kept as the name unless it is a unit`() {
        // "Joghurt" must not be eaten as a unit.
        assertSplit("2 Joghurt", "2", "Joghurt")
        assertSplit("1 Gurke", "1", "Gurke")
    }

    @Test
    fun `times notation`() {
        assertSplit("2x Joghurt", "2 x", "Joghurt")
        assertSplit("2 x Joghurt", "2 x", "Joghurt")
        assertSplit("Joghurt 2x", "2 x", "Joghurt")
    }

    @Test
    fun `fractions survive`() {
        assertSplit("1/2 Gurke", "1/2", "Gurke")
        assertSplit("½ Zitrone", "½", "Zitrone")
    }

    @Test
    fun `amount at the end`() {
        assertSplit("Milch 1 l", "1 l", "Milch")
        assertSplit("Mehl 1kg", "1 kg", "Mehl")
        assertSplit("Pizza 3 Stück", "3 Stück", "Pizza")
    }

    @Test
    fun `decimal separators are normalised to a comma`() {
        assertSplit("1.5 l Milch", "1,5 l", "Milch")
    }

    @Test
    fun `text without an amount is left alone`() {
        assertSplit("Tomaten", "", "Tomaten")
        assertSplit("Rote Bete", "", "Rote Bete")
        assertSplit("Gouda 45", "", "Gouda 45")
    }

    @Test
    fun `a stray number is not turned into an amount with no name`() {
        assertSplit("3", "", "3")
        assertSplit("500 g", "", "500 g")
    }

    @Test
    fun `pure quantities are recognised as such`() {
        assertTrue(QuantityParser.isPureQuantity("1 l"))
        assertTrue(QuantityParser.isPureQuantity("500 g"))
        assertTrue(QuantityParser.isPureQuantity("2"))
        assertTrue(QuantityParser.isPureQuantity("2x"))
        assertTrue(QuantityParser.isPureQuantity("2 Packungen"))
        assertFalse(QuantityParser.isPureQuantity("2 Joghurt"))
        assertFalse(QuantityParser.isPureQuantity("Lachs"))
        assertFalse(QuantityParser.isPureQuantity(""))
    }
}
