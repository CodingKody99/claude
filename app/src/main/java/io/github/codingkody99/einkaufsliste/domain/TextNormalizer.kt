package io.github.codingkody99.einkaufsliste.domain

/**
 * Folds item names into a lookup key: lower case, umlauts expanded, punctuation
 * dropped. "Bio-Vollmilch 3,5 %" and "bio vollmilch 3 5" land on the same key.
 */
object TextNormalizer {

    private val diacritics = mapOf(
        'ä' to "a", 'ö' to "o", 'ü' to "u",
        'á' to "a", 'à' to "a", 'â' to "a", 'ã' to "a", 'å' to "a",
        'é' to "e", 'è' to "e", 'ê' to "e", 'ë' to "e",
        'í' to "i", 'ì' to "i", 'î' to "i", 'ï' to "i",
        'ó' to "o", 'ò' to "o", 'ô' to "o", 'õ' to "o",
        'ú' to "u", 'ù' to "u", 'û' to "u",
        'ç' to "c", 'ñ' to "n", 'ý' to "y",
    )

    fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw.lowercase()) {
            when {
                ch == 'ß' -> sb.append("ss")
                diacritics.containsKey(ch) -> sb.append(diacritics[ch])
                ch in 'a'..'z' || ch in '0'..'9' -> sb.append(ch)
                else -> sb.append(' ')
            }
        }
        return sb.toString().trim().replace(WHITESPACE, " ")
    }

    /**
     * Candidate singular forms for a normalized German word, most specific
     * first. Deliberately a small set of suffix strips rather than a full
     * stemmer: the lexicon carries the common plurals itself, and these rules
     * only need to catch the rest ("kartoffeln" → "kartoffel").
     */
    fun singularCandidates(word: String): List<String> {
        val out = LinkedHashSet<String>()
        for (suffix in PLURAL_SUFFIXES) {
            if (word.length >= suffix.length + MIN_STEM && word.endsWith(suffix)) {
                out += word.dropLast(suffix.length)
            }
        }
        out -= word
        return out.toList()
    }

    private const val MIN_STEM = 3
    private val PLURAL_SUFFIXES = listOf("nen", "en", "er", "n", "e", "s")
    private val WHITESPACE = Regex("\\s+")
}
