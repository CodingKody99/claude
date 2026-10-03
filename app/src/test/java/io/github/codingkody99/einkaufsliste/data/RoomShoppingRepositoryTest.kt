package io.github.codingkody99.einkaufsliste.data

import io.github.codingkody99.einkaufsliste.domain.TextNormalizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomShoppingRepositoryTest {

    private val dao = FakeShoppingDao()
    private val overrideDao = FakeCategoryOverrideDao()
    private val listDao = FakeShoppingListDao()
    private var clock = 1_000L
    private val repository = RoomShoppingRepository(dao, overrideDao, listDao) { clock }

    private val listId = ShoppingList.DEFAULT_ID

    @Test
    fun `add stores the item unchecked and stamped with the current time`() = runTest {
        clock = 4_242L
        val id = repository.add(listId, NewItem("Tomaten", "500 g", Category.OBST_GEMUESE))

        val stored = dao.items.single()
        assertEquals(stored.id, id)
        assertEquals("Tomaten", stored.name)
        assertEquals("500 g", stored.quantity)
        assertEquals(Category.OBST_GEMUESE, stored.category)
        assertEquals(4_242L, stored.createdAt)
        assertFalse(stored.isChecked)
    }

    @Test
    fun `add trims surrounding whitespace`() = runTest {
        repository.add(listId, NewItem("  Milch  ", "  2 l  ", Category.MOLKEREI))

        val stored = dao.items.single()
        assertEquals("Milch", stored.name)
        assertEquals("2 l", stored.quantity)
    }

    @Test
    fun `add ignores a blank name and reports no id`() = runTest {
        assertNull(repository.add(listId, NewItem("   ", "1")))
        assertNull(repository.add(listId, NewItem("")))
        assertTrue(dao.items.isEmpty())
    }

    @Test
    fun `addAll keeps the given order and returns every new id`() = runTest {
        val ids = repository.addAll(
            listId,
            listOf(
                NewItem("Tomaten", category = Category.OBST_GEMUESE),
                NewItem("Milch", category = Category.MOLKEREI),
                NewItem("Brot", category = Category.BACKWAREN),
            ),
        )

        assertEquals(3, ids.size)
        assertEquals(listOf("Tomaten", "Milch", "Brot"), dao.items.map { it.name })
        assertEquals(dao.items.map { it.id }, ids)
    }

    @Test
    fun `addAll skips blank names without losing the other ids`() = runTest {
        val ids = repository.addAll(listId, listOf(NewItem("Milch"), NewItem("  "), NewItem("Brot")))

        assertEquals(2, ids.size)
        assertEquals(listOf("Milch", "Brot"), dao.items.map { it.name })
    }

    @Test
    fun `setChecked toggles only the targeted item`() = runTest {
        repository.add(listId, NewItem("Brot", category = Category.BACKWAREN))
        repository.add(listId, NewItem("Milch", category = Category.MOLKEREI))
        val (brot, milch) = dao.items

        repository.setChecked(brot.id, true)

        assertTrue(dao.items.first { it.id == brot.id }.isChecked)
        assertFalse(dao.items.first { it.id == milch.id }.isChecked)
    }

    @Test
    fun `update keeps id and created time but replaces the rest`() = runTest {
        repository.add(listId, NewItem("Brot", "1", Category.BACKWAREN))
        val original = dao.items.single()

        repository.update(original, "  Vollkornbrot ", " 2 Stück ", Category.VORRAT)

        val updated = dao.items.single()
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals("Vollkornbrot", updated.name)
        assertEquals("2 Stück", updated.quantity)
        assertEquals(Category.VORRAT, updated.category)
    }

    @Test
    fun `update ignores a blank name instead of wiping the item`() = runTest {
        repository.add(listId, NewItem("Brot", category = Category.BACKWAREN))
        val original = dao.items.single()

        repository.update(original, "   ", "", Category.SONSTIGES)

        assertEquals(original, dao.items.single())
    }

    @Test
    fun `deleteChecked removes only checked items`() = runTest {
        repository.add(listId, NewItem("Brot", category = Category.BACKWAREN))
        repository.add(listId, NewItem("Milch", category = Category.MOLKEREI))
        repository.setChecked(dao.items.first().id, true)

        repository.deleteChecked(listId)

        assertEquals(listOf("Milch"), dao.items.map { it.name })
    }

    @Test
    fun `restore brings items back under fresh ids and keeps their state`() = runTest {
        repository.add(listId, NewItem("Brot", "1 Stück", Category.BACKWAREN))
        val original = dao.items.single()
        repository.setChecked(original.id, true)
        val deleted = dao.items.single()
        repository.delete(deleted.id)
        assertTrue(dao.items.isEmpty())

        repository.restore(listOf(deleted))

        val restored = dao.items.single()
        assertEquals(deleted.name, restored.name)
        assertEquals(deleted.quantity, restored.quantity)
        assertEquals(deleted.category, restored.category)
        assertEquals(deleted.createdAt, restored.createdAt)
        assertTrue(restored.isChecked)
    }

    @Test
    fun `clearList empties the list`() = runTest {
        repository.add(listId, NewItem("Brot"))
        repository.add(listId, NewItem("Milch"))

        repository.clearList(listId)

        assertTrue(dao.items.isEmpty())
    }

    @Test
    fun `a remembered category is keyed by the normalized name`() = runTest {
        repository.rememberCategory("  Räucher-Tofu ", Category.MOLKEREI)

        assertEquals(
            mapOf(TextNormalizer.normalize("Räucher Tofu") to Category.MOLKEREI),
            repository.observeOverrides().first(),
        )
    }

    @Test
    fun `remembering the same name twice replaces the category`() = runTest {
        repository.rememberCategory("Yuzu", Category.OBST_GEMUESE)
        repository.rememberCategory("Yuzu", Category.VORRAT)

        assertEquals(1, overrideDao.rows.size)
        assertEquals(Category.VORRAT, repository.observeOverrides().first().values.single())
    }

    @Test
    fun `a blank name is never remembered`() = runTest {
        repository.rememberCategory("   ", Category.VORRAT)
        repository.rememberCategory("-", Category.VORRAT)

        assertTrue(overrideDao.rows.isEmpty())
    }

    @Test
    fun `forgetting removes the override again`() = runTest {
        repository.rememberCategory("Yuzu", Category.OBST_GEMUESE)
        repository.forgetCategory("yuzu")

        assertTrue(repository.observeOverrides().first().isEmpty())
    }
}
