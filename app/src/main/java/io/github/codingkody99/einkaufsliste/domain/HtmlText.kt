package io.github.codingkody99.einkaufsliste.domain

/**
 * The little bit of HTML handling a recipe page needs: entities and stray tags
 * inside otherwise plain text. Kept free of any parser dependency — the data we
 * read is JSON-LD, and this only cleans up what sites put inside those strings.
 */
object HtmlText {

    /** Decodes named and numeric entities; unknown ones are left as they are. */
    fun decodeEntities(input: String): String {
        if ('&' !in input) return input
        return ENTITY.replace(input) { match ->
            val body = match.groupValues[1]
            when {
                body.startsWith("#x", ignoreCase = true) ->
                    body.drop(2).toIntOrNull(16)?.let(::codePoint) ?: match.value

                body.startsWith("#") ->
                    body.drop(1).toIntOrNull()?.let(::codePoint) ?: match.value

                else -> NAMED[body] ?: NAMED[body.lowercase()] ?: match.value
            }
        }
    }

    fun stripTags(input: String): String = TAG.replace(input, " ")

    fun collapseWhitespace(input: String): String =
        input.replace(WHITESPACE, " ").trim()

    /** Entities, then tags, then whitespace — the order a text snippet needs. */
    fun toPlainText(input: String): String =
        collapseWhitespace(decodeEntities(stripTags(decodeEntities(input))))

    private fun codePoint(value: Int): String? =
        if (value in 1..0x10FFFF) String(Character.toChars(value)) else null

    private val ENTITY = Regex("&(#x?[0-9A-Fa-f]+|[A-Za-z][A-Za-z0-9]{1,31});")
    private val TAG = Regex("<[^>]*>")
    private val WHITESPACE = Regex("[\\s\\u00a0\\u200b]+")

    private val NAMED: Map<String, String> = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"",
        "apos" to "'", "#39" to "'", "nbsp" to " ", "ensp" to " ", "emsp" to " ",
        "shy" to "", "zwnj" to "", "zwj" to "",
        "auml" to "ä", "ouml" to "ö", "uuml" to "ü",
        "Auml" to "Ä", "Ouml" to "Ö", "Uuml" to "Ü", "szlig" to "ß",
        "eacute" to "é", "egrave" to "è", "ecirc" to "ê",
        "agrave" to "à", "acirc" to "â", "ccedil" to "ç",
        "ocirc" to "ô", "icirc" to "î", "ucirc" to "û", "ntilde" to "ñ",
        "deg" to "°", "middot" to "·", "bull" to "•",
        "ndash" to "–", "mdash" to "—", "hellip" to "…",
        "times" to "×", "frasl" to "/",
        "frac12" to "½", "frac14" to "¼", "frac34" to "¾",
        "sup1" to "¹", "sup2" to "²", "sup3" to "³",
        "euro" to "€", "pound" to "£", "copy" to "©", "reg" to "®",
        "laquo" to "«", "raquo" to "»", "bdquo" to "„", "ldquo" to "“", "rdquo" to "”",
        "sbquo" to "‚", "lsquo" to "‘", "rsquo" to "’",
    )
}
