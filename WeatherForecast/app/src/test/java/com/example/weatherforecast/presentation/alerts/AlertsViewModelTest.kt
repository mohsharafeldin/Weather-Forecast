package com.example.weatherforecast.presentation.alerts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.weatherforecast.data.model.WeatherAlert
import com.example.weatherforecast.data.repository.IAlertsRepository
import com.example.weatherforecast.presentation.settings.ISettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: IAlertsRepository
    private lateinit var alertScheduler: IAlertScheduler
    private lateinit var settingsDataStore: ISettingsDataStore
    private lateinit var viewModel: AlertsViewModel

    private val alertsFlow = MutableStateFlow<List<WeatherAlert>>(emptyList())

    private val testAlert = WeatherAlert(
        id = 1,
        startTime = 1000L,
        endTime = 2000L,
        alertType = "NOTIFICATION",
        isEnabled = true,
        snoozeDuration = 5
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        alertScheduler = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)

        every { repository.allAlerts } returns alertsFlow
        every { settingsDataStore.mapLat } returns flowOf(30.0)
        every { settingsDataStore.mapLon } returns flowOf(31.0)

        viewModel = AlertsViewModel(repository, alertScheduler, settingsDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should collect alerts into Success state`() = runTest {
        alertsFlow.value = listOf(testAlert)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlertsUiState.Success)
        assertEquals(1, (state as AlertsUiState.Success).alerts.size)
    }

    @Test
    fun `init with empty alerts should emit Success with empty list`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlertsUiState.Success)
        assertEquals(0, (state as AlertsUiState.Success).alerts.size)
    }

    @Test
    fun `addAlert should call repository and scheduler`() = runTest {
        coEvery { repository.addAlert(any()) } returns 5L
        advanceUntilIdle()

        viewModel.addAlert(1000L, 2000L, "NOTIFICATION", 5)
        advanceUntilIdle()

        coVerify { repository.addAlert(any()) }
        verify { alertScheduler.schedule(any(), 30.0, 31.0) }
    }

    @Test
    fun `removeAlert should call repository and cancel scheduler`() = runTest {
        coEvery { repository.removeAlert(any()) } just Runs
        advanceUntilIdle()

        viewModel.removeAlert(testAlert)
        advanceUntilIdle()

        coVerify { repository.removeAlert(testAlert) }
        verify { alertScheduler.cancel(testAlert) }
    }

    @Test
    fun `toggleAlert should disable and cancel when currently enabled`() = runTest {
        coEvery { repository.updateAlert(any()) } just Runs
        advanceUntilIdle()

        viewModel.toggleAlert(testAlert)
        advanceUntilIdle()

        coVerify {
            repository.updateAlert(match { !it.isEnabled })
        }
        verify { alertScheduler.cancel(any()) }
    }

    @Test
    fun `toggleAlert should enable and schedule when currently disabled`() = runTest {
        val disabledAlert = testAlert.copy(isEnabled = false)
        coEvery { repository.updateAlert(any()) } just Runs
        advanceUntilIdle()

        viewModel.toggleAlert(disabledAlert)
        advanceUntilIdle()

        coVerify {
            repository.updateAlert(match { it.isEnabled })
        }
        verify { alertScheduler.schedule(any(), 30.0, 31.0) }
    }
}
