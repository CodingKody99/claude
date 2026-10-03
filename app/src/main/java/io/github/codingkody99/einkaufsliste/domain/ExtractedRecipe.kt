package io.github.codingkody99.einkaufsliste.domain

/** What a recipe page gave us. */
data class ExtractedRecipe(
    val title: String,
    val ingredients: List<String>,
    /** The method, one step per line, when the page publishes it. */
    val instructions: String,
    val servings: String,
    val source: Source,
) {
    enum class Source {
        /** schema.org Recipe in a <script type="application/ld+json"> block. */
        JSON_LD,

        /** schema.org Recipe expressed as itemprop attributes in the markup. */
        MICRODATA,
    }
}
