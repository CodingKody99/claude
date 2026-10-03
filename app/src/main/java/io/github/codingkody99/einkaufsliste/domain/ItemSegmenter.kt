package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category

/**
 * Splits a run of words that carries several items but no punctuation.
 *
 * Dictation produces exactly that: "einen Tofu eine Gurke Reispapier
 * Reisnudeln" arrives as one line, because speech recognition does not insert
 * commas. Splitting on every known food would be wrong — "Rote Bete" and
 * "Brauner Zucker" are single items — so a boundary needs three things at once:
 *
 *  1. the current word starts something: it is a known food, an article or a
 *     number,
 *  2. what has been collected so far already contains a known food, and
 *  3. a known food still follows.
 *
 * Condition 3 is what keeps trailing amounts attached: in "Milch 1 l" the "1"
 * starts nothing, because no food comes after it.
 */
class ItemSegmenter(
    private val classifier: CategoryClassifier = CategoryClassifier(),
) {

    fun segment(text: String, overrides: Map<String, Category> = emptyMap()): List<String> {
        val tokens = text.trim().split(WHITESPACE).filter { it.isNotBlank() }
        if (tokens.size < 2) return tokens

        val isFood = tokens.map { classifier.recognizes(it, overrides) }

        val pieces = mutableListOf<String>()
        var current = mutableListOf<String>()
        var currentHasFood = false

        tokens.forEachIndexed { index, token ->
            val starts = isFood[index] || isCounter(token)
            val foodFollows = (index until tokens.size).any { isFood[it] }

            if (current.isNotEmpty() && currentHasFood && starts && foodFollows) {
                pieces += current.joinToString(" ")
                current = mutableListOf()
                currentHasFood = false
            }

            current += token
            if (isFood[index]) currentHasFood = true
        }

        if (current.isNotEmpty()) pieces += current.joinToString(" ")
        return pieces
    }

    /** Articles and numbers introduce the next item when one is already open. */
    private fun isCounter(token: String): Boolean {
        if (token.first().isDigit()) return true
        return TextNormalizer.normalize(token) in COUNTER_WORDS
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")

        val COUNTER_WORDS = setOf(
            "ein", "eine", "einen", "einem", "einer", "eins",
            "zwei", "drei", "vier", "funf", "sechs", "sieben", "acht", "neun",
            "zehn", "elf", "zwolf",
            "ein paar", "mehrere",
        )
    }
}
