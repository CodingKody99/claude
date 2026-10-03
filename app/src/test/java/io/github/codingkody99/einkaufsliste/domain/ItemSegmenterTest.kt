package io.github.codingkody99.einkaufsliste.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemSegmenterTest {

    private val segmenter = ItemSegmenter()

    private fun segment(text: String) = segmenter.segment(text)

    @Test
    fun `a single item stays whole`() {
        assertEquals(listOf("Tomaten"), segment("Tomaten"))
        assertEquals(listOf("Vollkornbrot"), segment("Vollkornbrot"))
        assertEquals(emptyList<String>(), segment("   "))
    }

    @Test
    fun `articles separate dictated items`() {
        assertEquals(listOf("einen Tofu", "eine Gurke"), segment("einen Tofu eine Gurke"))
        assertEquals(
            listOf("ein Brot", "zwei Tomaten"),
            segment("ein Brot zwei Tomaten"),
        )
    }

    @Test
    fun `consecutive known foods are separate items`() {
        assertEquals(listOf("Reispapier", "Reisnudeln"), segment("Reispapier Reisnudeln"))
        assertEquals(listOf("Milch", "Brot", "Tomaten"), segment("Milch Brot Tomaten"))
        assertEquals(listOf("Salz", "Pfeffer"), segment("Salz Pfeffer"))
    }

    @Test
    fun `a qualifier in front stays with its noun`() {
        assertEquals(listOf("Rote Bete"), segment("Rote Bete"))
        assertEquals(listOf("Brauner Zucker"), segment("Brauner Zucker"))
        assertEquals(listOf("Griechischer Joghurt"), segment("Griechischer Joghurt"))
        assertEquals(listOf("frische Tomaten"), segment("frische Tomaten"))
        assertEquals(listOf("Grüne Bohnen"), segment("Grüne Bohnen"))
    }

    @Test
    fun `a qualifier behind stays with its noun`() {
        assertEquals(listOf("Mozzarella Minis"), segment("Mozzarella Minis"))
        assertEquals(listOf("Apfelsaft naturtrüb"), segment("Apfelsaft naturtrüb"))
        assertEquals(listOf("Oliven grün"), segment("Oliven grün"))
    }

    @Test
    fun `a trailing amount is not torn off its item`() {
        // The lookahead is what saves this: no food follows the "1".
        assertEquals(listOf("Milch 1 l"), segment("Milch 1 l"))
        assertEquals(listOf("Pizza 3 Stück"), segment("Pizza 3 Stück"))
        assertEquals(listOf("Bio Vollmilch 3 5"), segment("Bio Vollmilch 3 5"))
    }

    @Test
    fun `a leading amount stays with the item that follows`() {
        assertEquals(listOf("500 g Hackfleisch"), segment("500 g Hackfleisch"))
        assertEquals(listOf("1 Prise Salz"), segment("1 Prise Salz"))
        assertEquals(listOf("2 Dosen Tomaten"), segment("2 Dosen Tomaten"))
    }

    @Test
    fun `amounts separate the items they belong to`() {
        assertEquals(
            listOf("500 g Hackfleisch", "2 Tomaten"),
            segment("500 g Hackfleisch 2 Tomaten"),
        )
    }

    @Test
    fun `unknown words are kept with the item they sit next to`() {
        assertEquals(listOf("Sojasauce hell"), segment("Sojasauce hell"))
        assertEquals(listOf("Gemüsebrühe Pulver"), segment("Gemüsebrühe Pulver"))
        assertEquals(listOf("Flurbereinigungsgerät"), segment("Flurbereinigungsgerät"))
    }

    @Test
    fun `a learned word counts as a known food for splitting`() {
        val overrides = mapOf(TextNormalizer.normalize("Yuzu") to
            io.github.codingkody99.einkaufsliste.data.Category.OBST_GEMUESE)
        assertEquals(listOf("Yuzu Yuzu"), segmenter.segment("Yuzu Yuzu"))
        assertEquals(listOf("Yuzu", "Yuzu"), segmenter.segment("Yuzu Yuzu", overrides))
    }
}
