package com.codegrogu.guava.viewmodel

import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val viewModel = AppViewModel()
        val state = viewModel.uiState.value
        
        assertNull(state.currentUser)
        assertEquals(UserRole.UNKNOWN, state.userRole)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `showSnackbar emits UiEvent correctly`() = runTest {
        val viewModel = AppViewModel()
        val message = "Error occurred"
        
        viewModel.showSnackbar(message, isError = true)
        
        val event = viewModel.uiEvents.first() as UiEvent.ShowSnackbar
        assertEquals(message, event.message)
        assertEquals(true, event.isError)
    }

    @Test
    fun `updateUser updates state correctly`() {
        val viewModel = AppViewModel()
        val user = User("1", "Test User", "test@example.com")
        val role = UserRole.MECHANIC
        
        viewModel.updateUser(user, role)
        
        val state = viewModel.uiState.value
        assertEquals(user, state.currentUser)
        assertEquals(role, state.userRole)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `setLoading updates state correctly`() {
        val viewModel = AppViewModel()
        
        viewModel.setLoading(true)
        assertEquals(true, viewModel.uiState.value.isLoading)
        
        viewModel.setLoading(false)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clearState resets state to default`() {
        val viewModel = AppViewModel()
        val user = User("1", "Test User", "test@example.com")
        
        viewModel.updateUser(user, UserRole.MANAGER)
        viewModel.clearState()
        
        val state = viewModel.uiState.value
        assertNull(state.currentUser)
        assertEquals(UserRole.UNKNOWN, state.userRole)
        assertEquals(false, state.isLoading)
    }
}
