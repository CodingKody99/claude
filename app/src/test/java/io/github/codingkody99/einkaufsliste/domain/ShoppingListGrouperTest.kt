package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category
import io.github.codingkody99.einkaufsliste.data.ShoppingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListGrouperTest {

    private fun item(
        id: Long,
        name: String,
        category: Category = Category.SONSTIGES,
        checked: Boolean = false,
        createdAt: Long = id,
    ) = ShoppingItem(
        id = id,
        name = name,
        category = category,
        isChecked = checked,
        createdAt = createdAt,
    )

    @Test
    fun `empty input produces no rows`() {
        assertEquals(emptyList<ShoppingListRow>(), ShoppingListGrouper.group(emptyList()))
    }

    @Test
    fun `open items are grouped under their category header`() {
        val rows = ShoppingListGrouper.group(
            listOf(
                item(1, "Brot", Category.BACKWAREN),
                item(2, "Tomaten", Category.OBST_GEMUESE),
                item(3, "Brötchen", Category.BACKWAREN),
            ),
        )

        // Category declaration order puts Obst & Gemüse ahead of Backwaren.
        assertEquals(
            listOf(
                "header-OBST_GEMUESE",
                "item-2",
                "header-BACKWAREN",
                "item-1",
                "item-3",
            ),
            rows.map { it.key },
        )
    }

    @Test
    fun `category header counts the items in its section`() {
        val rows = ShoppingListGrouper.group(
            listOf(
                item(1, "Brot", Category.BACKWAREN),
                item(2, "Brötchen", Category.BACKWAREN),
            ),
        )

        val header = rows.first() as ShoppingListRow.CategoryHeader
        assertEquals(Category.BACKWAREN, header.category)
        assertEquals(2, header.openCount)
    }

    @Test
    fun `checked items move to a single done section at the end`() {
        val rows = ShoppingListGrouper.group(
            listOf(
                item(1, "Brot", Category.BACKWAREN, checked = true),
                item(2, "Tomaten", Category.OBST_GEMUESE),
                item(3, "Milch", Category.MOLKEREI, checked = true),
            ),
        )

        assertEquals(
            listOf("header-OBST_GEMUESE", "item-2", "header-done", "item-1", "item-3"),
            rows.map { it.key },
        )
        assertEquals(2, (rows[2] as ShoppingListRow.DoneHeader).count)
    }

    @Test
    fun `no done section when nothing is checked`() {
        val rows = ShoppingListGrouper.group(listOf(item(1, "Brot")))
        assertTrue(rows.none { it is ShoppingListRow.DoneHeader })
    }

    @Test
    fun `items within a category keep insertion order`() {
        val rows = ShoppingListGrouper.group(
            listOf(
                item(id = 10, name = "zweitens", createdAt = 200),
                item(id = 11, name = "erstens", createdAt = 100),
            ),
        )

        val names = rows.filterIsInstance<ShoppingListRow.Entry>().map { it.item.name }
        assertEquals(listOf("erstens", "zweitens"), names)
    }

    @Test
    fun `items added in the same millisecond fall back to id order`() {
        val rows = ShoppingListGrouper.group(
            listOf(
                item(id = 2, name = "b", createdAt = 500),
                item(id = 1, name = "a", createdAt = 500),
            ),
        )

        val names = rows.filterIsInstance<ShoppingListRow.Entry>().map { it.item.name }
        assertEquals(listOf("a", "b"), names)
    }

    @Test
    fun `row keys are unique so LazyColumn can key on them`() {
        val rows = ShoppingListGrouper.group(
            listOf(
                item(1, "Brot", Category.BACKWAREN),
                item(2, "Tomaten", Category.OBST_GEMUESE),
                item(3, "Milch", Category.MOLKEREI, checked = true),
            ),
        )

        val keys = rows.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
    }
}
