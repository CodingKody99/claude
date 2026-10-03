package io.github.codingkody99.einkaufsliste.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `category survives a store and load cycle`() {
        Category.entries.forEach { category ->
            val stored = converters.fromCategory(category)
            assertEquals(category, converters.toCategory(stored))
        }
    }

    @Test
    fun `a null column reads back as the default category`() {
        assertEquals(Category.DEFAULT, converters.toCategory(null))
    }
}
