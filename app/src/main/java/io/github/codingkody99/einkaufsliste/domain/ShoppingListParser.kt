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
        // "500 g" or "2 x" on its own carries no item.
        if (QuantityParser.isPureQuantity(cleaned)) return null

        val (quantity, remainder) = QuantityParser.split(cleaned)
        val name = cleanName(remainder)
        if (name.none { it.isLetter() }) return null

        return entry(name, quantity, sourceLine, overrides, heading = false)
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

    private fun cleanName(input: String): String {
        val collapsed = input.trim().trim(*TRIM_CHARS).replace(WHITESPACE, " ").trim()
        // "tomaten" -> "Tomaten"; names that already start upper case stay put.
        return collapsed.replaceFirstChar { it.uppercaseChar() }
    }

    private fun splitFragments(body: String): List<String> =
        SPLIT.split(body).filter { it.isNotBlank() }

    private companion object {
        const val MAX_CAPTION_WORDS = 6

        val BULLET = Regex("^[-–—*•·‣▪●○+]+\\s*")
        val NUMBERING = Regex("^\\d{1,2}[.)]\\s+")
        val CHECKBOX = Regex("^(\\[[ xX]?]|☐|☑|✓|✔)\\s*")
        val WHITESPACE = Regex("\\s+")
        val TRIM_CHARS = charArrayOf(
            ' ', ',', ';', '.', '-', '–', ':', '!', '?', '*', '•', '%', '(', ')',
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
