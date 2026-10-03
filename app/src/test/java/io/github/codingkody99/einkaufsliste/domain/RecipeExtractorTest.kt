package io.github.codingkody99.einkaufsliste.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeExtractorTest {

    private val extractor = RecipeExtractor()

    /** How a German recipe site lays out a page: @graph, @type array, entities. */
    private val chefkochStylePage = """
        <!DOCTYPE html><html lang="de"><head>
        <title>Der perfekte Pfannkuchen - gelingt einfach immer | Chefkoch</title>
        <script type="application/ld+json">
        {"@context":"https://schema.org","@graph":[
          {"@type":"WebSite","name":"Chefkoch"},
          {"@type":"BreadcrumbList","itemListElement":[]},
          {"@type":["Recipe","CreativeWork"],
           "name":"Der perfekte Pfannkuchen - gelingt einfach immer",
           "recipeYield":"4 Portionen",
           "recipeIngredient":["250 g Mehl","3 Ei(er)","500 ml Milch",
             "1 Prise Salz","2 EL Zucker","etwas Butter&nbsp;zum Braten"],
           "recipeInstructions":"Alles verr&uuml;hren."}
        ]}
        </script>
        </head><body><h1>Pfannkuchen</h1></body></html>
    """.trimIndent()

    @Test
    fun `ingredients are read from a graph-style page`() {
        val recipe = extractor.extract(chefkochStylePage)!!

        assertEquals("Der perfekte Pfannkuchen - gelingt einfach immer", recipe.title)
        assertEquals("4 Portionen", recipe.servings)
        assertEquals(ExtractedRecipe.Source.JSON_LD, recipe.source)
        assertEquals(
            listOf(
                "250 g Mehl", "3 Ei(er)", "500 ml Milch",
                "1 Prise Salz", "2 EL Zucker", "etwas Butter zum Braten",
            ),
            recipe.ingredients,
        )
    }

    @Test
    fun `a recipe at the top level is found`() {
        val html = """
            <script type="application/ld+json">
            {"@context":"http://schema.org","@type":"Recipe","name":"Nudelauflauf",
             "recipeIngredient":["500 g Nudeln","200 g Käse"]}
            </script>
        """.trimIndent()
        val recipe = extractor.extract(html)!!
        assertEquals("Nudelauflauf", recipe.title)
        assertEquals(listOf("500 g Nudeln", "200 g Käse"), recipe.ingredients)
    }

    @Test
    fun `a recipe inside a top-level array is found`() {
        val html = """
            <script type="application/ld+json">
            [{"@type":"Organization","name":"Seite"},
             {"@type":"Recipe","name":"Suppe","recipeIngredient":["1 l Brühe"]}]
            </script>
        """.trimIndent()
        assertEquals(listOf("1 l Brühe"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `the recipe block is found even when another block comes first`() {
        val html = """
            <script type="application/ld+json">{"@type":"WebPage","name":"Start"}</script>
            <script type="application/ld+json">
            {"@type":"Recipe","name":"Brot","recipeIngredient":["500 g Mehl"]}</script>
        """.trimIndent()
        assertEquals(listOf("500 g Mehl"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `attribute spelling and order do not matter`() {
        val html = """
            <script data-x="1" type='application/ld+json' id="rezept">
            {"@type":"Recipe","name":"X","recipeIngredient":["1 Ei"]}</script>
        """.trimIndent()
        assertEquals(listOf("1 Ei"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `a single ingredient given as a plain string is accepted`() {
        val html = """
            <script type="application/ld+json">
            {"@type":"Recipe","name":"X","recipeIngredient":"1 Ei"}</script>
        """.trimIndent()
        assertEquals(listOf("1 Ei"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `the legacy ingredients field is used when recipeIngredient is absent`() {
        val html = """
            <script type="application/ld+json">
            {"@type":"Recipe","name":"Alt","ingredients":["200 g Zucker"]}</script>
        """.trimIndent()
        assertEquals(listOf("200 g Zucker"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `tags inside an ingredient line are flattened`() {
        val html = """
            <script type="application/ld+json">
            {"@type":"Recipe","name":"X","recipeIngredient":[
              "<span class=\"amount\">200 g</span> <span>Mehl</span>"]}</script>
        """.trimIndent()
        assertEquals(listOf("200 g Mehl"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `blank ingredient lines are dropped`() {
        val html = """
            <script type="application/ld+json">
            {"@type":"Recipe","name":"X","recipeIngredient":["200 g Mehl","","   "]}</script>
        """.trimIndent()
        assertEquals(listOf("200 g Mehl"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `microdata is used when there is no JSON-LD`() {
        val html = """
            <html><head><title>Pfannkuchen – Rezept</title></head><body>
            <div itemscope itemtype="http://schema.org/Recipe">
              <h1 itemprop="name">Pfannkuchen</h1>
              <li itemprop="recipeIngredient">250 g Mehl</li>
              <li itemprop="recipeIngredient">3 Eier</li>
            </div></body></html>
        """.trimIndent()
        val recipe = extractor.extract(html)!!
        assertEquals(ExtractedRecipe.Source.MICRODATA, recipe.source)
        assertEquals("Pfannkuchen", recipe.title)
        assertEquals(listOf("250 g Mehl", "3 Eier"), recipe.ingredients)
    }

    @Test
    fun `the legacy microdata ingredients attribute also works`() {
        val html = """<li itemprop="ingredients">1 Prise Salz</li>"""
        assertEquals(listOf("1 Prise Salz"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `a page without recipe data yields nothing`() {
        assertNull(extractor.extract("<html><body><p>Kein Rezept hier.</p></body></html>"))
        assertNull(extractor.extract(""))
    }

    @Test
    fun `a recipe without ingredients is treated as no recipe`() {
        val html = """
            <script type="application/ld+json">
            {"@type":"Recipe","name":"Leer","recipeInstructions":"nichts"}</script>
        """.trimIndent()
        assertNull(extractor.extract(html))
    }

    @Test
    fun `broken JSON does not take the whole page down`() {
        val html = """
            <script type="application/ld+json">{ this is not json </script>
            <script type="application/ld+json">
            {"@type":"Recipe","name":"Gut","recipeIngredient":["1 Ei"]}</script>
        """.trimIndent()
        assertEquals(listOf("1 Ei"), extractor.extract(html)!!.ingredients)
    }

    @Test
    fun `a missing title does not prevent the import`() {
        val html = """
            <script type="application/ld+json">
            {"@type":"Recipe","recipeIngredient":["1 Ei"]}</script>
        """.trimIndent()
        val recipe = extractor.extract(html)!!
        assertEquals("", recipe.title)
        assertTrue(recipe.ingredients.isNotEmpty())
    }

    @Test
    fun `a yield given as a number is read as text`() {
        val html = """
            <script type="application/ld+json">
            {"@type":"Recipe","name":"X","recipeYield":4,"recipeIngredient":["1 Ei"]}</script>
        """.trimIndent()
        assertEquals("4", extractor.extract(html)!!.servings)
    }

    @Test
    fun `the extracted ingredients feed straight into the list parser`() {
        val recipe = extractor.extract(chefkochStylePage)!!
        val entries = ShoppingListParser().parse(recipe.ingredients.joinToString("\n"))

        assertEquals(
            listOf("Mehl", "Ei", "Milch", "Salz", "Zucker", "Butter"),
            entries.map { it.name },
        )
        assertEquals(
            listOf("250 g", "3", "500 ml", "1 Prise", "2 EL", ""),
            entries.map { it.quantity },
        )
    }
}
