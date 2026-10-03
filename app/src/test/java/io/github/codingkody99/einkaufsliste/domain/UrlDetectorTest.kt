package io.github.codingkody99.einkaufsliste.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlDetectorTest {

    @Test
    fun `a bare link is found`() {
        assertEquals(
            "https://www.chefkoch.de/rezepte/123/Pfannkuchen.html",
            UrlDetector.firstUrl("https://www.chefkoch.de/rezepte/123/Pfannkuchen.html"),
        )
    }

    @Test
    fun `a link shared with its title is found`() {
        val shared = "Der perfekte Pfannkuchen | Chefkoch\nhttps://www.chefkoch.de/rezepte/123/x.html"
        assertEquals("https://www.chefkoch.de/rezepte/123/x.html", UrlDetector.firstUrl(shared))
    }

    @Test
    fun `trailing punctuation is not part of the link`() {
        assertEquals("https://a.de/x", UrlDetector.firstUrl("Schau mal: https://a.de/x."))
        assertEquals("https://a.de/x", UrlDetector.firstUrl("(https://a.de/x)"))
    }

    @Test
    fun `query strings and fragments are kept`() {
        val url = "https://a.de/rezept?id=7&utm_source=app#zutaten"
        assertEquals(url, UrlDetector.firstUrl(url))
    }

    @Test
    fun `a shopping list is not mistaken for a link`() {
        assertNull(UrlDetector.firstUrl("Milch\nBrot\nTomaten"))
        assertFalse(UrlDetector.containsUrl("Salat mit Käse"))
        // "www." without a scheme is not treated as a link, to stay predictable
        assertNull(UrlDetector.firstUrl("www.chefkoch.de"))
    }

    @Test
    fun `something too short to be a host is not a link`() {
        assertNull(UrlDetector.firstUrl("http://a"))
    }

    @Test
    fun `removing the link leaves the rest of the text`() {
        assertEquals(
            "Pfannkuchen",
            UrlDetector.withoutUrls("Pfannkuchen https://www.chefkoch.de/rezepte/1/x.html"),
        )
        assertEquals("", UrlDetector.withoutUrls("https://www.chefkoch.de/rezepte/1/x.html"))
    }

    @Test
    fun `containsUrl agrees with firstUrl`() {
        assertTrue(UrlDetector.containsUrl("hier https://a.de/rezept"))
        assertFalse(UrlDetector.containsUrl("Milch, Brot"))
    }
}
