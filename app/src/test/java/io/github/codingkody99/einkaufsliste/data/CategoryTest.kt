package io.github.codingkody99.einkaufsliste.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun `fromName round-trips every category`() {
        Category.entries.forEach { category ->
            assertEquals(category, Category.fromName(category.name))
        }
    }

    @Test
    fun `fromName falls back to the default for unknown or missing values`() {
        assertEquals(Category.DEFAULT, Category.fromName("DOES_NOT_EXIST"))
        assertEquals(Category.DEFAULT, Category.fromName(null))
        assertEquals(Category.DEFAULT, Category.fromName(""))
    }

    @Test
    fun `every category has a label and an emoji`() {
        Category.entries.forEach { category ->
            assertTrue(category.name, category.label.isNotBlank())
            assertTrue(category.name, category.emoji.isNotBlank())
        }
    }

    @Test
    fun `sonstiges sorts last so unassigned items land at the bottom`() {
        assertEquals(Category.SONSTIGES, Category.entries.last())
    }
}
