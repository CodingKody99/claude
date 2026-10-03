package io.github.codingkody99.einkaufsliste.ui

import io.github.codingkody99.einkaufsliste.data.FakeCategoryOverrideDao
import io.github.codingkody99.einkaufsliste.data.FakeRecipeDao
import io.github.codingkody99.einkaufsliste.data.FakeShoppingDao
import io.github.codingkody99.einkaufsliste.data.FakeShoppingListDao
import io.github.codingkody99.einkaufsliste.data.FakeSyncSettingsDao
import io.github.codingkody99.einkaufsliste.data.RoomShoppingRepository
import io.github.codingkody99.einkaufsliste.domain.HouseholdCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Joining two phones to the same household. */
@OptIn(ExperimentalCoroutinesApi::class)
class SharingTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeShoppingDao()
    private val overrideDao = FakeCategoryOverrideDao()
    private val listDao = FakeShoppingListDao()
    private val recipeDao = FakeRecipeDao()
    private val syncDao = FakeSyncSettingsDao()
    private var clock = 0L
    private val repository =
        RoomShoppingRepository(dao, overrideDao, listDao, recipeDao, syncDao) { ++clock }

    private lateinit var viewModel: ShoppingListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ShoppingListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lists stay on the device until sharing is started`() = runTest(dispatcher) {
        advanceUntilIdle()

        assertFalse(viewModel.sharing.value.isShared)
        assertNull(viewModel.sharing.value.householdCode)
        assertNull(syncDao.settings?.householdId)
    }

    @Test
    fun `starting sharing produces a code that can be passed on`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.startSharing()
        advanceUntilIdle()

        val code = viewModel.sharing.value.householdCode!!
        assertTrue(HouseholdCode.isValid(code))
        assertTrue(viewModel.sharing.value.isShared)
        assertEquals(code, syncDao.settings?.householdId)
    }

    @Test
    fun `the second phone joins with that code`() = runTest(dispatcher) {
        advanceUntilIdle()
        val code = HouseholdCode.generate()

        viewModel.openSharing()
        viewModel.onJoinCodeChange(HouseholdCode.format(code))
        viewModel.joinSharing()
        advanceUntilIdle()

        assertEquals(code, viewModel.sharing.value.householdCode)
        assertEquals(code, syncDao.settings?.householdId)
        assertFalse(viewModel.sharing.value.joinFailed)
    }

    @Test
    fun `a mistyped code is refused without changing anything`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openSharing()
        viewModel.onJoinCodeChange("ABC")
        viewModel.joinSharing()
        advanceUntilIdle()

        assertTrue(viewModel.sharing.value.joinFailed)
        assertNull(viewModel.sharing.value.householdCode)
        assertNull(syncDao.settings?.householdId)
    }

    @Test
    fun `correcting the code clears the complaint`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openSharing()
        viewModel.onJoinCodeChange("ABC")
        viewModel.joinSharing()
        advanceUntilIdle()
        assertTrue(viewModel.sharing.value.joinFailed)

        viewModel.onJoinCodeChange("ABCD")

        assertFalse(viewModel.sharing.value.joinFailed)
    }

    @Test
    fun `an empty code does nothing at all`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openSharing()
        viewModel.joinSharing()
        advanceUntilIdle()

        assertFalse(viewModel.sharing.value.joinFailed)
        assertNull(syncDao.settings?.householdId)
    }

    @Test
    fun `sharing can be stopped again`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startSharing()
        advanceUntilIdle()
        assertTrue(viewModel.sharing.value.isShared)

        viewModel.stopSharing()
        advanceUntilIdle()

        assertFalse(viewModel.sharing.value.isShared)
        assertNull(viewModel.sharing.value.householdCode)
    }

    @Test
    fun `the household survives a restart of the app`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.startSharing()
        advanceUntilIdle()
        val code = viewModel.sharing.value.householdCode

        val reopened = ShoppingListViewModel(repository)
        advanceUntilIdle()

        assertEquals(code, reopened.sharing.value.householdCode)
    }

    @Test
    fun `opening the sheet clears a previous attempt`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.openSharing()
        viewModel.onJoinCodeChange("ABC")
        viewModel.joinSharing()
        advanceUntilIdle()

        viewModel.dismissSharing()
        viewModel.openSharing()

        assertEquals("", viewModel.sharing.value.joinCode)
        assertFalse(viewModel.sharing.value.joinFailed)
        assertTrue(viewModel.sharing.value.visible)
    }
}
