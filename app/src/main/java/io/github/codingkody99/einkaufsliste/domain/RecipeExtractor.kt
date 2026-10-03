package io.github.codingkody99.einkaufsliste.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads the ingredients out of a recipe page.
 *
 * Recipe sites publish their recipes as schema.org data so that search engines
 * can show rich results — so that is what we read, rather than scraping the
 * visible markup, which every site builds differently and changes often.
 *
 * Two carriers for the same vocabulary are supported:
 *  1. JSON-LD in a `<script type="application/ld+json">` block (what nearly all
 *     sites use today). The object can sit anywhere in the document: at the top
 *     level, inside an array, or buried in an `@graph`, so it is searched for
 *     recursively.
 *  2. Microdata `itemprop` attributes, as a fallback for older pages.
 *
 * Returns null when the page carries no recipe data at all, so the caller can
 * say so instead of importing an empty list.
 */
class RecipeExtractor {

    fun extract(html: String): ExtractedRecipe? =
        fromJsonLd(html) ?: fromMicrodata(html)

    // --- JSON-LD --------------------------------------------------------------

    private fun fromJsonLd(html: String): ExtractedRecipe? {
        for (match in JSON_LD_BLOCK.findAll(html)) {
            val raw = HtmlText.decodeEntities(match.groupValues[1]).trim()
            if (raw.isEmpty()) continue
            val root = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: continue
            val recipe = findRecipe(root) ?: continue

            val ingredients = cleanIngredients(
                stringsOf(recipe["recipeIngredient"]).ifEmpty { stringsOf(recipe["ingredients"]) },
            )
            if (ingredients.isEmpty()) continue

            return ExtractedRecipe(
                title = textOf(recipe["name"]) ?: textOf(recipe["headline"]).orEmpty(),
                ingredients = ingredients,
                instructions = instructionsOf(recipe["recipeInstructions"]),
                servings = textOf(recipe["recipeYield"]).orEmpty(),
                source = ExtractedRecipe.Source.JSON_LD,
            )
        }
        return null
    }

    /** Depth-first search for an object whose `@type` includes "Recipe". */
    private fun findRecipe(element: JsonElement): JsonObject? = when (element) {
        is JsonObject ->
            if (isRecipe(element)) element
            else element.values.firstNotNullOfOrNull { findRecipe(it) }

        is JsonArray -> element.firstNotNullOfOrNull { findRecipe(it) }
        else -> null
    }

    private fun isRecipe(obj: JsonObject): Boolean =
        typeNames(obj["@type"]).any { it.equals("Recipe", ignoreCase = true) }

    /** `@type` is a string on most sites and an array on some. */
    private fun typeNames(element: JsonElement?): List<String> = when (element) {
        null, JsonNull -> emptyList()
        is JsonPrimitive -> listOf(element.content)
        is JsonArray -> element.flatMap { typeNames(it) }
        is JsonObject -> emptyList()
    }

    /** Flattens whatever shape a text-ish field arrived in. */
    private fun stringsOf(element: JsonElement?): List<String> = when (element) {
        null, JsonNull -> emptyList()
        is JsonPrimitive -> listOf(element.content)
        is JsonArray -> element.flatMap { stringsOf(it) }
        // Some sites wrap values as { "@value": ... } or { "name": ... }.
        is JsonObject -> stringsOf(element["@value"]).ifEmpty { stringsOf(element["name"]) }
    }

    /**
     * `recipeInstructions` is a free-for-all: a paragraph, a list of strings, a
     * list of HowToStep objects, or sections holding those. Collecting every
     * text value in order handles all of them.
     */
    private fun instructionsOf(element: JsonElement?): String =
        instructionTexts(element)
            .map(HtmlText::toPlainText)
            .filter { it.isNotBlank() }
            .joinToString("\n")

    private fun instructionTexts(element: JsonElement?): List<String> = when (element) {
        null, JsonNull -> emptyList()
        is JsonPrimitive -> listOf(element.content)
        is JsonArray -> element.flatMap { instructionTexts(it) }
        // A HowToSection holds its steps; a HowToStep holds its text.
        is JsonObject ->
            element["itemListElement"]?.let { instructionTexts(it) }
                ?: stringsOf(element["text"]).ifEmpty { stringsOf(element["name"]) }
    }

    private fun textOf(element: JsonElement?): String? =
        stringsOf(element).firstOrNull()?.let(HtmlText::toPlainText)?.takeIf { it.isNotEmpty() }

    // --- Microdata ------------------------------------------------------------

    private fun fromMicrodata(html: String): ExtractedRecipe? {
        val ingredients = cleanIngredients(
            MICRODATA_INGREDIENT.findAll(html).map { it.groupValues[2] }.toList(),
        )
        if (ingredients.isEmpty()) return null
        return ExtractedRecipe(
            title = microdataTitle(html),
            ingredients = ingredients,
            instructions = cleanIngredients(
                MICRODATA_INSTRUCTION.findAll(html).map { it.groupValues[2] }.toList(),
            ).joinToString("\n"),
            servings = "",
            source = ExtractedRecipe.Source.MICRODATA,
        )
    }

    private fun microdataTitle(html: String): String =
        MICRODATA_NAME.find(html)?.groupValues?.get(1)
            ?.let(HtmlText::toPlainText)
            ?.takeIf { it.isNotEmpty() }
            ?: HTML_TITLE.find(html)?.groupValues?.get(1)
                ?.let(HtmlText::toPlainText)
                .orEmpty()

    // --- shared ---------------------------------------------------------------

    /**
     * Ingredient lines arrive with tags, entities and sometimes amount and name
     * split across spans, so each line is flattened to plain text. Duplicates
     * are kept: a recipe may legitimately list sugar twice.
     */
    private fun cleanIngredients(raw: List<String>): List<String> =
        raw.map(HtmlText::toPlainText).filter { it.isNotBlank() }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private companion object {
        val JSON_LD_BLOCK = Regex(
            "<script[^>]*type\\s*=\\s*[\"']?application/ld\\+json[\"']?[^>]*>(.*?)</script>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        /** Group 1 is the tag name (closed by the back reference), group 2 the text. */
        val MICRODATA_INGREDIENT = Regex(
            "<([a-z]+)[^>]*itemprop\\s*=\\s*[\"'](?:recipeIngredient|ingredients)[\"'][^>]*>(.*?)</\\1>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        /** Group 1 is the tag name, group 2 the step text. */
        val MICRODATA_INSTRUCTION = Regex(
            "<([a-z]+)[^>]*itemprop\\s*=\\s*[\"'](?:recipeInstructions|instructions)[\"'][^>]*>(.*?)</\\1>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )

        val MICRODATA_NAME = Regex(
            "<[a-z]+[^>]*itemprop\\s*=\\s*[\"']name[\"'][^>]*>(.*?)</",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val HTML_TITLE = Regex(
            "<title[^>]*>(.*?)</title>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
