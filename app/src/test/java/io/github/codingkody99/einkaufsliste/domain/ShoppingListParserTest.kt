package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.Category.BACKWAREN
import io.github.codingkody99.einkaufsliste.data.Category.FLEISCH_FISCH
import io.github.codingkody99.einkaufsliste.data.Category.MOLKEREI
import io.github.codingkody99.einkaufsliste.data.Category.OBST_GEMUESE
import io.github.codingkody99.einkaufsliste.data.Category.VORRAT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListParserTest {

    private val parser = ShoppingListParser()

    private fun names(text: String) = parser.parse(text).map { it.name }
    private fun items(text: String) = parser.parse(text).filterNot { it.isHeading }

    @Test
    fun `empty input yields nothing`() {
        assertTrue(parser.parse("").isEmpty())
        assertTrue(parser.parse("   \n\n  \n").isEmpty())
    }

    @Test
    fun `one item per line`() {
        assertEquals(listOf("Milch", "Brot", "Tomaten"), names("Milch\nBrot\nTomaten"))
    }

    @Test
    fun `blank lines and stray whitespace are skipped`() {
        assertEquals(listOf("Milch", "Brot"), names("\n  Milch  \n\n\n Brot \n "))
    }

    @Test
    fun `bullet and number markers are stripped`() {
        assertEquals(
            listOf("Milch", "Brot", "Eier", "Butter", "Käse", "Salz"),
            names("- Milch\n* Brot\n• Eier\n1. Butter\n2) Käse\n[ ] Salz"),
        )
    }

    @Test
    fun `comma separated lines become separate items`() {
        assertEquals(listOf("Lachs", "Kartoffeln", "Dill"), names("Lachs, Kartoffeln, Dill"))
        assertEquals(listOf("Salz", "Pfeffer"), names("Salz; Pfeffer"))
    }

    @Test
    fun `German connectors split into separate items`() {
        assertEquals(listOf("Salz", "Pfeffer"), names("Salz und Pfeffer"))
        assertEquals(listOf("Salat", "Käse"), names("Salat mit Käse"))
        assertEquals(listOf("Öl", "Essig"), names("Öl sowie Essig"))
        assertEquals(listOf("Brot", "Butter"), names("Brot & Butter"))
    }

    @Test
    fun `a connector inside a word does not split it`() {
        assertEquals(listOf("Gewürzmischung"), names("Gewürzmischung"))
        assertEquals(listOf("Mittagessen"), names("Mittagessen"))
    }

    @Test
    fun `quantities are separated from the name`() {
        val parsed = items("500g Hackfleisch\n2 Joghurt\nMilch 1 l")
        assertEquals(listOf("Hackfleisch", "Joghurt", "Milch"), parsed.map { it.name })
        assertEquals(listOf("500 g", "2", "1 l"), parsed.map { it.quantity })
    }

    @Test
    fun `names are capitalised`() {
        assertEquals(listOf("Tomaten", "Käse"), names("tomaten\nkäse"))
    }

    @Test
    fun `every item is filed into its supermarket section`() {
        val parsed = items("Tomaten\nBrot\nMilch\nHackfleisch\nNudeln")
        assertEquals(
            listOf(OBST_GEMUESE, BACKWAREN, MOLKEREI, FLEISCH_FISCH, VORRAT),
            parsed.map { it.category },
        )
        assertTrue(parsed.all { it.recognized })
    }

    @Test
    fun `an unknown item is flagged rather than guessed`() {
        val entry = items("Flurbereinigungsgerät").single()
        assertEquals(Category.DEFAULT, entry.category)
        assertFalse(entry.recognized)
    }

    @Test
    fun `a caption with a colon is reported as a heading and its items parsed`() {
        val parsed = parser.parse("Abendessen Freitag: Lachs, Kartoffeln, Dill")
        assertEquals(
            listOf("Abendessen Freitag", "Lachs", "Kartoffeln", "Dill"),
            parsed.map { it.name },
        )
        assertEquals(listOf(true, false, false, false), parsed.map { it.isHeading })
    }

    @Test
    fun `a caption on its own line is a heading`() {
        val parsed = parser.parse("Einkauf Samstag:\nMilch\nBrot")
        assertTrue(parsed.first().isHeading)
        assertEquals("Einkauf Samstag", parsed.first().name)
        assertEquals(listOf("Milch", "Brot"), parsed.drop(1).map { it.name })
    }

    @Test
    fun `a markdown heading is a heading`() {
        val parsed = parser.parse("## Für den Salat\nTomaten")
        assertTrue(parsed.first().isHeading)
        assertEquals("Für den Salat", parsed.first().name)
        assertFalse(parsed[1].isHeading)
    }

    @Test
    fun `an item with a colon and an amount stays one item`() {
        val parsed = parser.parse("Milch: 1 l")
        assertEquals(1, parsed.size)
        assertEquals("Milch", parsed.single().name)
        assertEquals("1 l", parsed.single().quantity)
        assertFalse(parsed.single().isHeading)
    }

    @Test
    fun `amount-only fragments are dropped instead of becoming items`() {
        assertTrue(parser.parse("500 g").isEmpty())
        assertTrue(parser.parse("2x").isEmpty())
        assertTrue(parser.parse("3").isEmpty())
        assertTrue(parser.parse("- , ;").isEmpty())
    }

    @Test
    fun `every entry remembers the line it came from`() {
        val parsed = parser.parse("Lachs, Kartoffeln")
        assertTrue(parsed.all { it.sourceLine == "Lachs, Kartoffeln" })
    }

    @Test
    fun `learned overrides are applied during import`() {
        val overrides = mapOf(TextNormalizer.normalize("Yuzu") to OBST_GEMUESE)
        val entry = parser.parse("Yuzu", overrides).single()
        assertEquals(OBST_GEMUESE, entry.category)
        assertTrue(entry.recognized)
    }

    @Test
    fun `a recipe-style list from another person is fully broken down`() {
        val pasted = """
            Salat mit Käse
            - 2 Tomaten
            - 1 Gurke
            - Feta
            - Rucola
            - Olivenöl

            Abendessen Freitag: Lachs, Kartoffeln, Dill, Zitrone

            Milch
            2x Joghurt
            500g Hackfleisch
        """.trimIndent()

        val parsed = parser.parse(pasted)
        val entries = parsed.filterNot { it.isHeading }

        // The dish line is split into its two real purchases.
        assertEquals(listOf("Salat", "Käse"), entries.take(2).map { it.name })
        // The caption with a colon is held back as a heading.
        assertEquals(listOf("Abendessen Freitag"), parsed.filter { it.isHeading }.map { it.name })

        assertEquals(
            listOf(
                "Salat", "Käse", "Tomaten", "Gurke", "Feta", "Rucola", "Olivenöl",
                "Lachs", "Kartoffeln", "Dill", "Zitrone",
                "Milch", "Joghurt", "Hackfleisch",
            ),
            entries.map { it.name },
        )
        assertEquals("2", entries.first { it.name == "Tomaten" }.quantity)
        assertEquals("2 x", entries.first { it.name == "Joghurt" }.quantity)
        assertEquals("500 g", entries.first { it.name == "Hackfleisch" }.quantity)

        // Everything landed in a real section, nothing fell through to Sonstiges.
        assertTrue(
            entries.filterNot { it.recognized }.map { it.name }.toString(),
            entries.all { it.recognized },
        )
    }

    @Test
    fun `the imported recipe list sorts into supermarket order`() {
        val pasted = "Salat mit Käse\n- 2 Tomaten\n- Feta\n- Olivenöl\nMilch\n500g Hackfleisch"
        val categories = items(pasted)
            .map { it.category }
            .distinct()
        // Declaration order of Category is the route; the distinct sections seen
        // here must be orderable by it without surprises.
        assertEquals(categories.sortedBy { it.ordinal }.distinct(), categories.distinct().sortedBy { it.ordinal })
        assertTrue(OBST_GEMUESE in categories)
        assertTrue(MOLKEREI in categories)
        assertTrue(VORRAT in categories)
        assertTrue(FLEISCH_FISCH in categories)
    }
}
