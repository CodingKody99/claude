package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category

/**
 * Picks the supermarket section for an item name.
 *
 * Strategy, strongest evidence first:
 *  1. something the user corrected before (learned override)
 *  2. the whole name as a lexicon entry, also in singular
 *  3. a freezer marker — "TK-Spinat" is frozen food, not produce
 *  4. the longest word of the name that is a lexicon entry. Length decides
 *     rather than position, because German puts the head of a *compound* last
 *     ("rote Paprika") but the head of a *phrase* first ("Frischkäse Kräuter"),
 *     and the longer keyword is the more specific one either way
 *  5. the longest lexicon entry a word *ends with* — this is what makes
 *     "Vollkornbrot" bread and "Hafermilch" dairy without listing every compound
 *  6. the longest lexicon entry contained anywhere in the name
 *
 * Unknown names fall back to [Category.DEFAULT] rather than guessing.
 */
class CategoryClassifier(
    private val lexicon: Map<String, Category> = FoodLexicon.byKeyword,
    private val keysByLengthDesc: List<String> = FoodLexicon.keysByLengthDesc,
) {

    enum class Source {
        /** The user has corrected this name before. */
        LEARNED,

        /** The name, or one of its words, is a lexicon entry. */
        EXACT,

        /** Matched through a marker, compound or substring rule. */
        DERIVED,
    }

    data class Match(val category: Category, val keyword: String, val source: Source)

    fun classify(rawName: String, overrides: Map<String, Category> = emptyMap()): Category =
        match(rawName, overrides)?.category ?: Category.DEFAULT

    /** True when the name resolved to something better than the fallback. */
    fun recognizes(rawName: String, overrides: Map<String, Category> = emptyMap()): Boolean =
        match(rawName, overrides) != null

    fun match(rawName: String, overrides: Map<String, Category> = emptyMap()): Match? {
        val key = TextNormalizer.normalize(rawName)
        if (key.isEmpty()) return null

        // The whole name, exactly as written and in singular.
        lookup(key, overrides)?.let { return it }
        TextNormalizer.singularCandidates(key).forEach { candidate ->
            lookup(candidate, overrides)?.let { return it }
        }

        val words = key.split(' ').filter { it.isNotEmpty() }

        freezerMarker(key, words)?.let { return it }

        // Longest listed word wins, so a qualifier never outranks the real noun.
        words.mapNotNull { word -> bestWordMatch(word, overrides) }
            .maxByOrNull { it.keyword.length }
            ?.let { return it }

        // Compound tail: "vollkornbrot" ends with "brot".
        words.mapNotNull { compoundTail(it) }
            .maxByOrNull { it.keyword.length }
            ?.let { return it }

        // Last resort: the longest entry appearing anywhere in the name.
        keysByLengthDesc.firstOrNull { it.length >= MIN_SUBSTRING && key.contains(it) }
            ?.let { return Match(lexicon.getValue(it), it, Source.DERIVED) }

        return null
    }

    private fun bestWordMatch(word: String, overrides: Map<String, Category>): Match? {
        lookup(word, overrides)?.let { return it }
        TextNormalizer.singularCandidates(word).forEach { candidate ->
            lookup(candidate, overrides)?.let { return it }
        }
        return null
    }

    private fun lookup(key: String, overrides: Map<String, Category>): Match? {
        overrides[key]?.let { return Match(it, key, Source.LEARNED) }
        lexicon[key]?.let { return Match(it, key, Source.EXACT) }
        return null
    }

    /**
     * "Tiefkühlerbsen" and "TK-Spinat" belong in the freezer aisle, not with the
     * fresh vegetables their head word would suggest.
     */
    private fun freezerMarker(key: String, words: List<String>): Match? {
        if (words.any { it == "tk" }) return Match(Category.TIEFKUEHL, "tk", Source.DERIVED)
        val marker = FREEZER_MARKERS.firstOrNull { key.contains(it) } ?: return null
        return Match(Category.TIEFKUEHL, marker, Source.DERIVED)
    }

    private fun compoundTail(word: String): Match? {
        if (word.length <= MIN_COMPOUND_TAIL) return null
        val tail = keysByLengthDesc.firstOrNull { candidate ->
            candidate.length >= MIN_COMPOUND_TAIL &&
                candidate.length < word.length &&
                word.endsWith(candidate)
        } ?: return null
        return Match(lexicon.getValue(tail), tail, Source.DERIVED)
    }

    private companion object {
        val FREEZER_MARKERS = listOf("tiefkuhl", "tiefgekuhlt", "tiefgefroren", "gefroren")

        /**
         * Short tails produce nonsense ("Alkohol" would end with "öl"), so a
         * compound has to share at least this many characters with an entry.
         */
        const val MIN_COMPOUND_TAIL = 4

        /** Substring matching is the weakest rule, so it needs more evidence. */
        const val MIN_SUBSTRING = 5
    }
}
