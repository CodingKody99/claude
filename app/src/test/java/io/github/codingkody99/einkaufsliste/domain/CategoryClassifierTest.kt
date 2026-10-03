package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.Category.BACKWAREN
import io.github.codingkody99.einkaufsliste.data.Category.FLEISCH_FISCH
import io.github.codingkody99.einkaufsliste.data.Category.GETRAENKE
import io.github.codingkody99.einkaufsliste.data.Category.HAUSHALT
import io.github.codingkody99.einkaufsliste.data.Category.MOLKEREI
import io.github.codingkody99.einkaufsliste.data.Category.OBST_GEMUESE
import io.github.codingkody99.einkaufsliste.data.Category.SUESS_SNACKS
import io.github.codingkody99.einkaufsliste.data.Category.TIEFKUEHL
import io.github.codingkody99.einkaufsliste.data.Category.VORRAT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryClassifierTest {

    private val classifier = CategoryClassifier()

    private fun assertCategory(expected: Category, vararg names: String) {
        names.forEach { name ->
            val match = classifier.match(name)
            assertEquals(
                "\"$name\" -> ${match?.category} über \"${match?.keyword}\" (${match?.source})",
                expected,
                classifier.classify(name),
            )
        }
    }

    @Test
    fun `plain singular names resolve`() {
        assertCategory(OBST_GEMUESE, "Tomate", "Gurke", "Apfel", "Kartoffel", "Rucola")
        assertCategory(BACKWAREN, "Brot", "Brötchen", "Baguette")
        assertCategory(MOLKEREI, "Milch", "Käse", "Butter", "Joghurt", "Ei")
        assertCategory(FLEISCH_FISCH, "Hackfleisch", "Lachs", "Salami")
        assertCategory(VORRAT, "Nudeln", "Reis", "Mehl", "Salz")
        assertCategory(GETRAENKE, "Wasser", "Bier", "Apfelsaft")
        assertCategory(HAUSHALT, "Klopapier", "Spülmittel", "Zahnpasta")
        assertCategory(SUESS_SNACKS, "Schokolade", "Chips")
        assertCategory(TIEFKUEHL, "Pommes", "Eis")
    }

    @Test
    fun `plurals resolve to the same section`() {
        assertCategory(OBST_GEMUESE, "Tomaten", "Kartoffeln", "Zwiebeln", "Äpfel", "Gurken")
        assertCategory(MOLKEREI, "Eier", "Joghurts")
        assertCategory(BACKWAREN, "Brötchen", "Brote")
    }

    @Test
    fun `casing and spelling noise do not matter`() {
        assertCategory(MOLKEREI, "käse", "KÄSE", "  Käse  ", "Kaese".replace("ae", "ä"))
        assertCategory(MOLKEREI, "Bio-Vollmilch 3,5 %")
        assertCategory(OBST_GEMUESE, "TOMATEN")
    }

    @Test
    fun `adjectives before the noun are ignored because the head comes last`() {
        assertCategory(OBST_GEMUESE, "rote Paprika", "frische Tomaten", "grüner Salat")
        assertCategory(FLEISCH_FISCH, "gemischtes Hackfleisch")
        assertCategory(MOLKEREI, "griechischer Joghurt")
    }

    @Test
    fun `German compounds resolve through their head word`() {
        assertCategory(BACKWAREN, "Vollkornbrot", "Dinkelbrötchen", "Roggenmischbrot")
        assertCategory(MOLKEREI, "Hafermilch", "Ziegenjoghurt", "Schnittkäse")
        assertCategory(FLEISCH_FISCH, "Rinderhackfleisch", "Putenschnitzel", "Lachsfilet")
        assertCategory(GETRAENKE, "Kirschsaft", "Rhabarbersaft")
    }

    @Test
    fun `tricky compounds are pinned by explicit entries`() {
        // Each of these would land in the wrong aisle on the compound rule alone.
        assertCategory(VORRAT, "Tomatenmark", "Erdnussbutter", "Kokosmilch", "Milchreis")
        assertCategory(MOLKEREI, "Buttermilch", "Pizzateig")
        assertCategory(TIEFKUEHL, "Tiefkühlpizza", "Fischstäbchen", "Rahmspinat")
        assertCategory(VORRAT, "Pizzasauce", "Tomatensauce")
        assertCategory(GETRAENKE, "Tomatensaft")
    }

    @Test
    fun `in a phrase the longest listed word decides, not the last one`() {
        // "Frischkäse Kräuter" is a cream cheese, not fresh herbs: the head of a
        // phrase sits in front, unlike the head of a compound.
        assertCategory(MOLKEREI, "Frischkäse Kräuter", "Mozzarella Minis")
        assertCategory(GETRAENKE, "Apfelsaft naturtrüb", "Mineralwasser still")
        assertCategory(HAUSHALT, "Waschmittel flüssig")
        assertCategory(VORRAT, "Oliven grün")
        // and the compound case still works, where the head is the last part
        assertCategory(OBST_GEMUESE, "rote Paprika", "Saftorangen")
    }

    @Test
    fun `freezer markers win over the fresh section`() {
        assertCategory(TIEFKUEHL, "Tiefkühlerbsen", "TK-Spinat", "Tiefkühlkräuter")
        assertCategory(TIEFKUEHL, "Beerenmischung tiefgekühlt", "Erbsen tiefgefroren")
        // but plain fresh produce is untouched
        assertCategory(OBST_GEMUESE, "Spinat", "Erbsen")
    }

    @Test
    fun `a learned override still beats a freezer marker`() {
        val overrides = mapOf(TextNormalizer.normalize("TK-Spinat") to OBST_GEMUESE)
        assertEquals(OBST_GEMUESE, classifier.classify("TK-Spinat", overrides))
    }

    @Test
    fun `spreads sit with the preserves, not with the sweets`() {
        assertCategory(VORRAT, "Schokoaufstrich", "Brotaufstrich", "Tomatenpassata")
    }

    @Test
    fun `quantities left in the name do not break the lookup`() {
        assertCategory(MOLKEREI, "Milch 1 l", "2 Joghurt")
        assertCategory(OBST_GEMUESE, "500 g Tomaten")
    }

    @Test
    fun `unknown names fall back instead of guessing`() {
        assertEquals(Category.DEFAULT, classifier.classify("Flurbereinigungsgerät"))
        assertEquals(Category.DEFAULT, classifier.classify("xyzzy"))
        assertEquals(Category.DEFAULT, classifier.classify(""))
        assertEquals(Category.DEFAULT, classifier.classify("   "))
        assertNull(classifier.match("xyzzy"))
        assertFalse(classifier.recognizes("xyzzy"))
        assertTrue(classifier.recognizes("Tomaten"))
    }

    @Test
    fun `a learned override beats the lexicon`() {
        val overrides = mapOf(TextNormalizer.normalize("Tomaten") to HAUSHALT)
        assertEquals(HAUSHALT, classifier.classify("Tomaten", overrides))
        assertEquals(CategoryClassifier.Source.LEARNED, classifier.match("Tomaten", overrides)?.source)
        // and only for that name
        assertEquals(OBST_GEMUESE, classifier.classify("Gurken", overrides))
    }

    @Test
    fun `a learned override teaches a word the lexicon does not know`() {
        val overrides = mapOf(TextNormalizer.normalize("Yuzu") to OBST_GEMUESE)
        assertEquals(Category.DEFAULT, classifier.classify("Yuzu"))
        assertEquals(OBST_GEMUESE, classifier.classify("Yuzu", overrides))
    }

    @Test
    fun `an override also applies to the plural through singular folding`() {
        val overrides = mapOf(TextNormalizer.normalize("Yuzu") to OBST_GEMUESE)
        assertEquals(OBST_GEMUESE, classifier.classify("Yuzus", overrides))
    }

    @Test
    fun `exact entries report themselves as exact matches`() {
        assertEquals(CategoryClassifier.Source.EXACT, classifier.match("Tomaten")?.source)
        assertEquals(CategoryClassifier.Source.DERIVED, classifier.match("Dinkelbrötchen")?.source)
    }

    @Test
    fun `a short word is never matched by a compound tail`() {
        // "Alkohol" must not become cooking oil through a two-letter tail.
        assertEquals(Category.DEFAULT, classifier.classify("Alkohol"))
    }

    @Test
    fun `every lexicon keyword classifies back into its own category`() {
        val wrong = FoodLexicon.byKeyword.entries
            .filter { (keyword, category) -> classifier.classify(keyword) != category }
            .map { (keyword, category) -> "$keyword: erwartet $category, war ${classifier.classify(keyword)}" }
        assertTrue("Lexikon widerspricht sich: $wrong", wrong.isEmpty())
    }
}
