package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomShoppingRepositoryTest {

    private val dao = FakeShoppingDao()
    private var clock = 1_000L
    private val repository = RoomShoppingRepository(dao) { clock }

    @Test
    fun `add stores the item unchecked and stamped with the current time`() = runTest {
        clock = 4_242L
        repository.add("Tomaten", "500 g", Category.OBST_GEMUESE)

        val stored = dao.items.single()
        assertEquals("Tomaten", stored.name)
        assertEquals("500 g", stored.quantity)
        assertEquals(Category.OBST_GEMUESE, stored.category)
        assertEquals(4_242L, stored.createdAt)
        assertFalse(stored.isChecked)
    }

    @Test
    fun `add trims surrounding whitespace`() = runTest {
        repository.add("  Milch  ", "  2 l  ", Category.MOLKEREI)

        val stored = dao.items.single()
        assertEquals("Milch", stored.name)
        assertEquals("2 l", stored.quantity)
    }

    @Test
    fun `add ignores a blank name`() = runTest {
        repository.add("   ", "1", Category.SONSTIGES)
        repository.add("", "", Category.SONSTIGES)

        assertTrue(dao.items.isEmpty())
    }

    @Test
    fun `setChecked toggles only the targeted item`() = runTest {
        repository.add("Brot", "", Category.BACKWAREN)
        repository.add("Milch", "", Category.MOLKEREI)
        val (brot, milch) = dao.items

        repository.setChecked(brot.id, true)

        assertTrue(dao.items.first { it.id == brot.id }.isChecked)
        assertFalse(dao.items.first { it.id == milch.id }.isChecked)
    }

    @Test
    fun `rename keeps id and created time but updates the rest`() = runTest {
        repository.add("Brot", "1", Category.BACKWAREN)
        val original = dao.items.single()

        repository.rename(original, "  Vollkornbrot ", " 2 Stück ", Category.VORRAT)

        val updated = dao.items.single()
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals("Vollkornbrot", updated.name)
        assertEquals("2 Stück", updated.quantity)
        assertEquals(Category.VORRAT, updated.category)
    }

    @Test
    fun `rename ignores a blank name instead of wiping the item`() = runTest {
        repository.add("Brot", "", Category.BACKWAREN)
        val original = dao.items.single()

        repository.rename(original, "   ", "", Category.SONSTIGES)

        assertEquals(original, dao.items.single())
    }

    @Test
    fun `deleteChecked removes only checked items`() = runTest {
        repository.add("Brot", "", Category.BACKWAREN)
        repository.add("Milch", "", Category.MOLKEREI)
        repository.setChecked(dao.items.first().id, true)

        repository.deleteChecked()

        assertEquals(listOf("Milch"), dao.items.map { it.name })
    }

    @Test
    fun `restore brings items back under fresh ids and keeps their state`() = runTest {
        repository.add("Brot", "1 Stück", Category.BACKWAREN)
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
    fun `deleteAll empties the list`() = runTest {
        repository.add("Brot", "", Category.BACKWAREN)
        repository.add("Milch", "", Category.MOLKEREI)

        repository.deleteAll()

        assertTrue(dao.items.isEmpty())
    }
}
