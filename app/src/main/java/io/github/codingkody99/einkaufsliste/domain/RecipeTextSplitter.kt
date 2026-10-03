package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category

/**
 * Pulls the ingredients and the method apart from one blob of recipe text.
 *
 * A photographed recipe card is read column by column, so what arrives is the
 * cooking steps, the equipment list, the ingredient table and the nutrition
 * panel all in one run — and the nutrition panel in particular degrades into
 * fragments like "Brer" or "renmwert". Feeding that straight to a shopping list
 * produces a hundred items that nobody wants.
 *
 * Two rules do the separating:
 *
 *  1. Section headings mark where the ingredients start, and the next heading
 *     or a nutrition marker marks where they end.
 *  2. Inside that block only lines where the lexicon actually finds a food are
 *     kept. That is what filters the table debris out, since "Brennwert" split
 *     into "Brer" matches nothing while "Kirschtomaten" does.
 *
 * Everything outside the block becomes the method, minus obvious noise. The
 * method is held to a lower standard on purpose: it is read by a person, while
 * the ingredients drive the shopping list and have to be clean.
 */
class RecipeTextSplitter(
    private val classifier: CategoryClassifier = CategoryClassifier(),
) {

    data class Result(val ingredients: String, val steps: String) {
        val isEmpty: Boolean get() = ingredients.isBlank() && steps.isBlank()
    }

    fun split(text: String, overrides: Map<String, Category> = emptyMap()): Result {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return Result("", "")

        val ingredientsHeading = lines.indexOfFirst { INGREDIENTS_HEADING.containsMatchIn(key(it)) }
        val methodHeading = lines.indexOfFirst { METHOD_HEADING.containsMatchIn(key(it)) }

        // Without any heading there is nothing to separate on, and losing an
        // ingredient is worse than carrying a stray line, so keep it all.
        if (ingredientsHeading < 0 && methodHeading < 0) {
            return Result(lines.joinToString("\n"), "")
        }

        // Only the method is marked: everything before it is the ingredient part.
        if (ingredientsHeading < 0) {
            return Result(
                ingredients = lines.take(methodHeading).filterNot { isNoise(it) }
                    .joinToString("\n"),
                steps = lines.drop(methodHeading + 1).filterNot { isNoise(it) }.joinToString("\n"),
            )
        }

        val region = ingredientRegion(lines, ingredientsHeading)
        val ingredients = mutableListOf<String>()
        val steps = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            when {
                index == ingredientsHeading -> Unit
                // The heading is a label, not a step.
                METHOD_HEADING.containsMatchIn(key(line)) -> Unit
                index in region && looksLikeIngredient(line, overrides) -> ingredients += tidy(line)
                index in region -> Unit // table debris between the ingredients
                isNoise(line) -> Unit
                else -> steps += line
            }
        }

        return Result(ingredients.joinToString("\n"), steps.joinToString("\n"))
    }

    /** From just after the heading to the next heading or nutrition marker. */
    private fun ingredientRegion(lines: List<String>, heading: Int): IntRange {
        val end = (heading + 1 until lines.size).firstOrNull { index ->
            val normalized = key(lines[index])
            METHOD_HEADING.containsMatchIn(normalized) || HARD_STOP.containsMatchIn(normalized)
        } ?: lines.size
        return (heading + 1) until end
    }

    private fun looksLikeIngredient(line: String, overrides: Map<String, Category>): Boolean {
        if (isNoise(line)) return false
        // An ingredient is a name, not a sentence.
        if (line.split(' ').count { it.isNotBlank() } > MAX_INGREDIENT_WORDS) return false
        return classifier.recognizes(tidy(line), overrides)
    }

    /** Recipe cards footnote their ingredients: "Orzo-Nudeln 1)", "Hartkäse 2) 5)". */
    private fun tidy(line: String): String = FOOTNOTES.replace(line, "").trim()

    private fun isNoise(line: String): Boolean {
        val letters = line.count { it.isLetter() }
        if (letters < MIN_LETTERS) return true
        if (NOISE_WORDS.containsMatchIn(key(line))) return true

        // Rows of a nutrition or amount table: mostly figures, little else.
        val tokens = line.split(' ').filter { it.isNotBlank() }
        val figures = tokens.count { token -> token.any { it.isDigit() } }
        return tokens.isNotEmpty() && figures * 2 > tokens.size
    }

    private fun key(line: String) = TextNormalizer.normalize(line)

    private companion object {
        const val MAX_INGREDIENT_WORDS = 5
        const val MIN_LETTERS = 4

        val INGREDIENTS_HEADING = Regex(
            "^(?:zutaten|einkaufsliste|du brauchst|das brauchst du|ingredients)\\b",
        )

        val METHOD_HEADING = Regex(
            "^(?:zubereitung|los geht s|so geht s|und so geht s|anleitung|" +
                "zubereitungsschritte|schritte|preparation|method|instructions)\\b",
        )

        /** Anything from here on is no longer a list of things to buy. */
        val HARD_STOP = Regex(
            "^(?:durchschnittliche|nahrwerte|nahrwertangaben|brennwert|allergene|" +
                "ursprungsland|guten appetit|zubereiten)\\b",
        )

        val NOISE_WORDS = Regex(
            "nahrwert|brennwert|durchschnittlich|kcal|\\bkj\\b|allergen|erzeugnis|" +
                "ursprungsland|fettsauren|kohlenhydrate|eiweiss|laktose|sulfite|" +
                "rezeptkarte|produktanderung|schwankung|portion ca|gesattigte",
        )

        val FOOTNOTES = Regex("(?:\\s*\\d\\s*\\))+\\s*$")
    }
}
