package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodLexiconTest {

    @Test
    fun `no keyword is claimed by two categories`() {
        val owners = mutableMapOf<String, MutableSet<Category>>()
        FoodLexicon.declarations.forEach { (category, keywords) ->
            keywords.forEach { keyword ->
                owners.getOrPut(TextNormalizer.normalize(keyword)) { mutableSetOf() } += category
            }
        }
        val clashes = owners.filterValues { it.size > 1 }
        assertTrue("Schlüssel in mehreren Kategorien: $clashes", clashes.isEmpty())
    }

    @Test
    fun `no keyword normalizes to an empty key`() {
        val empty = FoodLexicon.declarations
            .flatMap { it.second }
            .filter { TextNormalizer.normalize(it).isEmpty() }
        assertTrue("Leere Schlüssel: $empty", empty.isEmpty())
    }

    @Test
    fun `every category except the fallback carries vocabulary`() {
        val covered = FoodLexicon.byKeyword.values.toSet()
        Category.entries.filter { it != Category.DEFAULT }.forEach {
            assertTrue("Keine Einträge für $it", it in covered)
        }
    }

    @Test
    fun `the fallback category has no vocabulary of its own`() {
        assertTrue(FoodLexicon.byKeyword.values.none { it == Category.DEFAULT })
    }

    @Test
    fun `the lexicon is large enough to be useful`() {
        assertTrue("nur ${FoodLexicon.byKeyword.size} Einträge", FoodLexicon.byKeyword.size >= 400)
    }

    @Test
    fun `keys are sorted longest first for compound matching`() {
        val lengths = FoodLexicon.keysByLengthDesc.map { it.length }
        assertEquals(lengths.sortedDescending(), lengths)
        assertEquals(FoodLexicon.byKeyword.size, FoodLexicon.keysByLengthDesc.size)
    }
}
