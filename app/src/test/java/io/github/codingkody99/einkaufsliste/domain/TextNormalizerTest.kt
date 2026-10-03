package io.github.codingkody99.einkaufsliste.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `lower cases and expands umlauts`() {
        assertEquals("kase", TextNormalizer.normalize("Käse"))
        assertEquals("apfel", TextNormalizer.normalize("Äpfel"))
        assertEquals("brotchen", TextNormalizer.normalize("Brötchen"))
        assertEquals("sussstoff", TextNormalizer.normalize("Süßstoff"))
        assertEquals("creme fraiche", TextNormalizer.normalize("Crème fraîche"))
    }

    @Test
    fun `strips punctuation and collapses whitespace`() {
        assertEquals("bio vollmilch 3 5", TextNormalizer.normalize("  Bio-Vollmilch 3,5 %  "))
        assertEquals("wc reiniger", TextNormalizer.normalize("WC-Reiniger"))
        assertEquals("rote bete", TextNormalizer.normalize("Rote   Bete"))
    }

    @Test
    fun `casing and padding do not change the key`() {
        assertEquals(TextNormalizer.normalize("Joghurt"), TextNormalizer.normalize("  joghurt "))
        assertEquals(TextNormalizer.normalize("TOMATEN"), TextNormalizer.normalize("Tomaten"))
    }

    @Test
    fun `empty and punctuation-only input normalize to empty`() {
        assertEquals("", TextNormalizer.normalize(""))
        assertEquals("", TextNormalizer.normalize("   "))
        assertEquals("", TextNormalizer.normalize("- * •"))
    }

    @Test
    fun `singular candidates cover the common German plurals`() {
        fun assertSingular(plural: String, singular: String) =
            assertTrue(
                "$plural -> $singular fehlt in ${TextNormalizer.singularCandidates(plural)}",
                TextNormalizer.singularCandidates(plural).contains(singular),
            )

        assertSingular("kartoffeln", "kartoffel")
        assertSingular("tomaten", "tomate")
        assertSingular("zwiebeln", "zwiebel")
        assertSingular("gurken", "gurke")
        assertSingular("brote", "brot")
        assertSingular("joghurts", "joghurt")
        assertSingular("wurste", "wurst")
        assertSingular("nudeln", "nudel")
    }

    @Test
    fun `stems shorter than three characters are not produced`() {
        // "eier" -> "ei" would be correct German but opens the door to junk
        // stems, so short irregulars are carried by the lexicon instead.
        assertTrue(TextNormalizer.singularCandidates("eier").isEmpty())
        assertTrue(TextNormalizer.singularCandidates("tee").isEmpty())
        TextNormalizer.singularCandidates("oliven").forEach {
            assertTrue(it, it.length >= 3)
        }
    }

    @Test
    fun `singular candidates never return the word itself or a stub`() {
        TextNormalizer.singularCandidates("tomaten").forEach {
            assertTrue(it, it != "tomaten")
            assertTrue(it, it.length >= 3)
        }
        assertTrue(TextNormalizer.singularCandidates("ei").isEmpty())
        assertTrue(TextNormalizer.singularCandidates("").isEmpty())
    }
}
