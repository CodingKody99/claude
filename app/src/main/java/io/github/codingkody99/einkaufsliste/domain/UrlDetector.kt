package io.github.codingkody99.einkaufsliste.domain

/**
 * Finds a link in pasted text. Android's share sheet usually hands over a title
 * and the link together, so the link has to be picked out of surrounding words.
 */
object UrlDetector {

    fun firstUrl(text: String): String? =
        URL.find(text)?.value?.trimEnd(*TRAILING)?.takeIf { it.length > MIN_LENGTH }

    fun containsUrl(text: String): Boolean = firstUrl(text) != null

    /** The text with the link removed, to tell a bare link from a real list. */
    fun withoutUrls(text: String): String = URL.replace(text, " ").trim()

    private const val MIN_LENGTH = 11 // "https://a.b"
    private val TRAILING = charArrayOf('.', ',', ';', ':', ')', ']', '}', '"', '\'', '!', '?', '>')
    private val URL = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
}
