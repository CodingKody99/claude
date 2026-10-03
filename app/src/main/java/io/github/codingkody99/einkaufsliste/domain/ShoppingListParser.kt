package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category

/**
 * Turns a pasted shopping list into entries, whatever shape it arrived in:
 * bullet lists, numbered lists, comma runs, quantities in front or behind, and
 * recipe captions like "Salat mit Käse" or "Abendessen Freitag:".
 *
 * Two deliberate choices:
 *  - A caption is reported as an entry marked [ParsedEntry.isHeading] rather
 *    than dropped, so the import preview can show what happened to every line.
 *  - "A mit B" and "A und B" become separate entries. On a shopping list
 *    "Salat mit Käse" means lettuce *and* cheese, and those sit in different
 *    aisles — which is the whole reason for importing.
 */
class ShoppingListParser(
    private val classifier: CategoryClassifier = CategoryClassifier(),
) {

    fun parse(text: String, overrides: Map<String, Category> = emptyMap()): List<ParsedEntry> {
        val entries = mutableListOf<ParsedEntry>()
        text.split('\n').forEach { line -> parseLine(line, overrides, entries) }
        return entries
    }

    private fun parseLine(
        rawLine: String,
        overrides: Map<String, Category>,
        into: MutableList<ParsedEntry>,
    ) {
        val line = rawLine.trim()
        if (line.isEmpty()) return

        val isMarkdownHeading = line.startsWith("#")
        var body = stripMarkers(line)
        if (body.isEmpty()) return

        val colon = body.indexOf(':')
        if (colon > 0) {
            val label = body.take(colon).trim()
            val rest = body.drop(colon + 1).trim()
            when {
                // "Einkauf:" on its own is a caption.
                rest.isEmpty() -> {
                    into += caption(label, line, overrides)
                    return
                }
                // "Milch: 1 l" is one item with an amount, not a caption.
                QuantityParser.isPureQuantity(rest) -> {
                    into += item(label, rest, line, overrides)
                    return
                }
                // "Abendessen Freitag: Lachs, Kartoffeln" is a caption plus items.
                isCaptionLabel(label) -> {
                    into += caption(label, line, overrides)
                    body = rest
                }
            }
        } else if (isMarkdownHeading) {
            into += caption(body, line, overrides)
            return
        }

        splitFragments(body).forEach { fragment ->
            toEntry(fragment, line, overrides)?.let { into += it }
        }
    }

    /** Captions are short; a long run before a colon is more likely a real line. */
    private fun isCaptionLabel(label: String): Boolean =
        label.isNotEmpty() && label.split(' ').count { it.isNotBlank() } <= MAX_CAPTION_WORDS

    private fun caption(
        text: String,
        sourceLine: String,
        overrides: Map<String, Category>,
    ): ParsedEntry = entry(cleanName(stripMarkers(text)), "", sourceLine, overrides, heading = true)

    private fun item(
        rawName: String,
        quantity: String,
        sourceLine: String,
        overrides: Map<String, Category>,
    ): ParsedEntry {
        val (parsedQuantity, remainder) = QuantityParser.split(quantity.trim())
        val amount = if (parsedQuantity.isNotEmpty() && remainder.isEmpty()) {
            parsedQuantity
        } else {
            quantity.trim()
        }
        return entry(cleanName(stripMarkers(rawName)), amount, sourceLine, overrides, heading = false)
    }

    private fun toEntry(
        fragment: String,
        sourceLine: String,
        overrides: Map<String, Category>,
    ): ParsedEntry? {
        val cleaned = stripMarkers(fragment.trim())
        if (cleaned.isEmpty()) return null

        // "Ich will Sommerrollen kochen" names the dish, not something to buy.
        dishName(cleaned)?.let { dish ->
            return entry(cleanName(dish), "", sourceLine, overrides, heading = true)
        }

        // Spoken input carries the sentence around the item: strip it before the
        // amount is read, or "dafür brauche ich 2 Karotten" hides its own "2".
        val spoken = stripSpokenFiller(cleaned)
        if (spoken.isEmpty()) return null
        // "500 g" or "2 x" on its own carries no item.
        if (QuantityParser.isPureQuantity(spoken)) return null

        val (quantity, remainder) = QuantityParser.split(spoken)
        val name = cleanName(remainder)
        if (name.none { it.isLetter() }) return null

        return entry(name, quantity, sourceLine, overrides, heading = false)
    }

    /** The dish in "ich will X kochen" / "wir machen heute X", if that is the shape. */
    private fun dishName(text: String): String? = DISH_SENTENCES
        .firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1) }
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    /** Removes conversational lead-ins, however many are stacked up. */
    private fun stripSpokenFiller(text: String): String {
        var current = text
        while (true) {
            val stripped = SPOKEN_PREFIX.replace(current, "").trim()
            if (stripped == current) return current
            current = stripped
        }
    }

    private fun entry(
        name: String,
        quantity: String,
        sourceLine: String,
        overrides: Map<String, Category>,
        heading: Boolean,
    ): ParsedEntry {
        val match = classifier.match(name, overrides)
        return ParsedEntry(
            name = name,
            quantity = quantity,
            category = match?.category ?: Category.DEFAULT,
            recognized = match != null,
            isHeading = heading,
            sourceLine = sourceLine,
        )
    }

    private fun stripMarkers(input: String): String {
        var current = input.trim()
        while (true) {
            val before = current
            current = current.removePrefix("#").trim()
            current = BULLET.replace(current, "").trim()
            current = NUMBERING.replace(current, "").trim()
            current = CHECKBOX.replace(current, "").trim()
            if (current == before) return current
        }
    }

    /**
     * Turns an ingredient or list line into the name you want on a shopping
     * list. Recipe wording carries three kinds of noise that are useless in a
     * shop: bracketed notes ("Ei(er)", "Mehl (Type 405)"), vague amounts
     * ("etwas", "evtl.") and what the ingredient is *for* ("zum Braten").
     */
    private fun cleanName(input: String): String {
        var name = BRACKETS.replace(input, " ")
        name = name.trim().trim(*TRIM_CHARS).replace(WHITESPACE, " ").trim()

        // Repeated, because "evtl. etwas Butter" stacks them.
        while (true) {
            val stripped = VAGUE_PREFIX.replace(name, "").trim()
            if (stripped == name) break
            name = stripped
        }

        name = PURPOSE_SUFFIX.replace(name, "").trim()
        name = name.trim(*TRIM_CHARS).trim()

        // "tomaten" -> "Tomaten"; names that already start upper case stay put.
        return name.replaceFirstChar { it.uppercaseChar() }
    }

    private fun splitFragments(body: String): List<String> =
        SPLIT.split(body).filter { it.isNotBlank() }

    private companion object {
        const val MAX_CAPTION_WORDS = 6

        val BULLET = Regex("^[-–—*•·‣▪●○+]+\\s*")
        val NUMBERING = Regex("^\\d{1,2}[.)]\\s+")
        val CHECKBOX = Regex("^(\\[[ xX]?]|☐|☑|✓|✔)\\s*")
        val WHITESPACE = Regex("\\s+")
        val BRACKETS = Regex("\\([^)]*\\)|\\[[^]]*]")

        /** Leading words that say "some" rather than how much. */
        val VAGUE_PREFIX = Regex(
            "^(?:etwas|etw\\.?|evtl\\.?|eventuell|ca\\.?|ggf\\.?|ein wenig|ein paar|" +
                "nach belieben|nach geschmack|n\\.\\s*b\\.?)(?:\\s+|$)",
            RegexOption.IGNORE_CASE,
        )

        /** Trailing phrases saying what the ingredient is for. */
        val PURPOSE_SUFFIX = Regex(
            "\\s+(?:zum|zur|fuer|fuers|für|fürs)\\s+.+$|" +
                "\\s*,?\\s*(?:nach belieben|nach geschmack|n\\.\\s*b\\.?)$",
            RegexOption.IGNORE_CASE,
        )
        val TRIM_CHARS = charArrayOf(
            ' ', ',', ';', '.', '-', '–', ':', '!', '?', '*', '•', '%', '(', ')',
        )

        /**
         * Sentences that announce a dish. The cooking verb is required, so
         * "ich will Milch" stays an item while "ich will Suppe kochen" does not.
         */
        val DISH_SENTENCES = listOf(
            Regex(
                "^(?:ich|wir)\\s+(?:will|wollen|möchte|möchten|werde|werden)\\s+(.+?)\\s+" +
                    "(?:kochen|machen|backen|zubereiten|grillen|braten)$",
                RegexOption.IGNORE_CASE,
            ),
            Regex(
                "^(?:ich|wir)\\s+(?:koche|kochen|backe|backen|mache|machen|grille|grillen)\\s+" +
                    "(?:heute|morgen|gleich|nachher)?\\s*(.+?)$",
                RegexOption.IGNORE_CASE,
            ),
            Regex(
                "^für\\s+(.+?)\\s+(?:brauche|brauchen|benötige|benötigen)\\s+(?:ich|wir)$",
                RegexOption.IGNORE_CASE,
            ),
        )

        /**
         * Lead-ins that dictation produces around the actual items. Applied
         * repeatedly, so "und dann brauche ich noch Milch" peels down to "Milch".
         */
        val SPOKEN_PREFIX = Regex(
            "^(?:" +
                "und|dann|danach|außerdem|ausserdem|auch|noch|bitte|also|dafür|dazu|ach ja|ach" +
                "|ich|wir" +
                "|brauche|brauchen|bräuchte|bräuchten|benötige|benötigen" +
                "|will|wollen|möchte|möchten|würde|würden|werde|werden|hätte|hätten" +
                "|kauf|kaufe|kaufen|besorg|besorge|besorgen|hol|hole|holen|nimm|nehme|nehmen" +
                ")\\b[\\s,]*",
            RegexOption.IGNORE_CASE,
        )

        /**
         * Separators that mean "another item": punctuation plus the German
         * connectors. The word connectors need surrounding whitespace so that
         * "Gewürzmischung" is not cut apart.
         */
        val SPLIT = Regex(
            "\\s*[,;]\\s*|\\s+(?:und|mit|sowie|plus)\\s+|\\s*&\\s*|\\s+\\+\\s+",
            RegexOption.IGNORE_CASE,
        )
    }
}
