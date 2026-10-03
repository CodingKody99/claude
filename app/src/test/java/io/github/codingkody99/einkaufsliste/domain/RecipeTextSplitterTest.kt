package io.github.codingkody99.einkaufsliste.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextSplitterTest {

    private val splitter = RecipeTextSplitter()

    /**
     * Verbatim output of the on-device text recogniser for a photographed
     * HelloFresh card, misreadings and all. This is the case the splitter
     * exists for, so it is tested against the real thing rather than a tidy
     * imitation.
     */
    private val scannedCard = """
        Los geht's
        Wasche Gemüse und Krauter ab.
        Erhtze 400 ml [600 ml| B00 ml Wasser im Wasserkocher.
        9.
        Heze den Backofen auf 220C Ober-/Unterhitze
        200CUmlut) vor.
        Was Du benötigst
        1 Backblech mit Backpapier, 1 große Pfanne mit Deckel,
        1 Knoblauchpresse, 1 Messbecher, 1 große Schüssel
        Zutaten 2 4 Personen
        Champignons
        ewurznischungHello
        Ziebel DE
        Knoblauchzehe ES
        Kirschtomaten
        to 11)
        Tomateptu
        Orzo-Nudeln 1)
        geriebener Hartkäse 2) 5)
        Basilikum/Thymian
        Mozzarella 5)
        Babyspinat
        Gemüsebrühe 3)
        Brer
        renmwert
        Aoo ees Fettsauren
        udate
        2P
        Zucker
        200
        42
        1
        2020W5
        1
        125 R
        25g
        200 g
        Durchschnittliche
        Nährwerte pro
        20 p
        108
        48
        jelEL
        300 g
        3P
        4,84 g
        68
        1,76g
        758
        68
        je 15 EL
        400 ml 600 ml
        1
        250g
        12,37 8
        50 g
        159 g
        300 g
        40g
        10g
        481 kJ/115 kcal
        Oi für Schritt l und 3
        Wasser für Schritt 2
        Salr,Pfeffer, Zucker
        5,23 g
        0,242 g
        4P
        400 g
        nach Geschmack
        100g
        3100 kJ/741 kcal
        31,19 g
        79,76g
        10,26 g
        33,71 g
        Ursprungsländers DE: Deutschland ES: Spanien
        1.560 g
        1
        Gemüse vorbereiten
        Champignons je nach Größe vierteln oder
        achteln, in eine große Schüssel geben, mit der
        Zwiebel und Knoblauch abziehen.
        Kirschtomaten halbieren.
        2
        Gemüse backen
        Thymianblätter abziehen.
        Basilikumblätter fein hacken (Stiele nicht
        wegwerfen).
        6
        Anrichten
        Orzo-Nudel-Risotto auf Teller verteilen, rauchige
        Champignons, gebackene Kirschtomaten und
        restlichen Hartkäse darauf anrichten.
        Guten Appetit
    """.trimIndent()

    @Test
    fun `the scanned card yields the ingredients, not the whole page`() {
        val result = splitter.split(scannedCard)
        val ingredients = result.ingredients.split('\n')

        // Eleven lines instead of the 150-odd the raw text produces.
        assertEquals(
            listOf(
                "Champignons",
                "Knoblauchzehe ES",
                "Kirschtomaten",
                "Tomateptu",
                "Orzo-Nudeln",
                "geriebener Hartkäse",
                "Basilikum/Thymian",
                "Mozzarella",
                "Babyspinat",
                "Gemüsebrühe",
                "Zucker",
            ),
            ingredients,
        )
    }

    @Test
    fun `footnote markers are removed from an ingredient`() {
        val ingredients = splitter.split(scannedCard).ingredients
        assertTrue(ingredients.contains("Orzo-Nudeln\n"))
        assertFalse("Fußnoten bleiben stehen", ingredients.contains("1)"))
        assertFalse(ingredients.contains("2) 5)"))
    }

    @Test
    fun `the cooking steps land in the method, not in the shopping list`() {
        val result = splitter.split(scannedCard)

        assertTrue(result.steps.contains("Wasche Gemüse und Krauter ab."))
        assertTrue(result.steps.contains("Gemüse vorbereiten"))
        assertTrue(result.steps.contains("Kirschtomaten halbieren."))
        assertTrue(result.steps.contains("Anrichten"))

        assertFalse("Zubereitung gehört nicht in die Zutaten", result.ingredients.contains("Wasche"))
        assertFalse(result.ingredients.contains("halbieren"))
        assertFalse(result.ingredients.contains("Backblech"))
    }

    @Test
    fun `the nutrition table is dropped entirely`() {
        val result = splitter.split(scannedCard)
        val everything = result.ingredients + "\n" + result.steps

        listOf("481 kJ", "4,84 g", "Durchschnittliche", "Nährwerte", "31,19 g", "1.560 g")
            .forEach { assertFalse(it, everything.contains(it)) }
    }

    @Test
    fun `garbled fragments do not become items`() {
        val ingredients = splitter.split(scannedCard).ingredients
        listOf("renmwert", "udate", "2020W5", "2P", "jelEL")
            .forEach { assertFalse(it, ingredients.contains(it)) }
    }

    @Test
    fun `a photo of nothing but an ingredient list is kept whole`() {
        // No heading to separate on, so nothing may be thrown away.
        val text = "Champignons\nGewürzmischung Hello Smokey\nZwiebel\nOrzo-Nudeln"
        val result = splitter.split(text)

        assertEquals(text, result.ingredients)
        assertEquals("", result.steps)
    }

    @Test
    fun `a marked method is separated even without an ingredient heading`() {
        val text = """
            Champignons
            Zwiebel
            Zubereitung
            Champignons vierteln.
            Zwiebel in Streifen schneiden.
        """.trimIndent()
        val result = splitter.split(text)

        assertEquals("Champignons\nZwiebel", result.ingredients)
        assertEquals("Champignons vierteln.\nZwiebel in Streifen schneiden.", result.steps)
    }

    @Test
    fun `a tidy card splits cleanly`() {
        val text = """
            Zutaten für 4 Personen
            250 g Mehl
            3 Eier
            500 ml Milch
            1 Prise Salz
            Zubereitung
            Mehl, Eier und Milch verrühren.
            In der Pfanne ausbacken.
        """.trimIndent()
        val result = splitter.split(text)

        assertEquals("250 g Mehl\n3 Eier\n500 ml Milch\n1 Prise Salz", result.ingredients)
        assertEquals(
            "Mehl, Eier und Milch verrühren.\nIn der Pfanne ausbacken.",
            result.steps,
        )
    }

    @Test
    fun `empty input stays empty`() {
        val result = splitter.split("   \n\n  ")
        assertTrue(result.isEmpty)
    }
}
