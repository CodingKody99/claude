package io.github.codingkody99.einkaufsliste.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {

    @Test
    fun `named entities are decoded`() {
        assertEquals("Öl & Essig", HtmlText.decodeEntities("&Ouml;l &amp; Essig"))
        assertEquals("½ TL", HtmlText.decodeEntities("&frac12; TL"))
        assertEquals("süß", HtmlText.decodeEntities("s&uuml;&szlig;"))
        assertEquals("„Mehl“", HtmlText.decodeEntities("&bdquo;Mehl&ldquo;"))
    }

    @Test
    fun `numeric entities are decoded in both notations`() {
        assertEquals("½", HtmlText.decodeEntities("&#189;"))
        assertEquals("½", HtmlText.decodeEntities("&#x00BD;"))
        assertEquals("ä", HtmlText.decodeEntities("&#228;"))
    }

    @Test
    fun `a non-breaking space becomes a normal one`() {
        assertEquals("200 g Mehl", HtmlText.toPlainText("200&nbsp;g&nbsp;Mehl"))
    }

    @Test
    fun `unknown entities are left untouched rather than mangled`() {
        assertEquals("&nosuchthing;", HtmlText.decodeEntities("&nosuchthing;"))
        assertEquals("100 % Vollkorn", HtmlText.decodeEntities("100 % Vollkorn"))
    }

    @Test
    fun `tags are stripped and whitespace collapsed`() {
        assertEquals(
            "200 g Mehl",
            HtmlText.toPlainText("<span class=\"amount\">200 g</span>\n  <span>Mehl</span>"),
        )
    }

    @Test
    fun `text without markup passes through unchanged`() {
        assertEquals("200 g Mehl", HtmlText.toPlainText("200 g Mehl"))
        assertEquals("", HtmlText.toPlainText("   "))
    }

    @Test
    fun `double-escaped entities are resolved`() {
        // JSON-LD inside HTML sometimes arrives escaped twice.
        assertEquals("Öl", HtmlText.toPlainText("&amp;Ouml;l"))
    }
}
