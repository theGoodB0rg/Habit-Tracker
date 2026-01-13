package com.habittracker.timerux

import com.habittracker.data.repository.HabitRepository
import com.habittracker.data.repository.timing.TimingRepository
import com.habittracker.data.preferences.TimingPreferencesRepository
import com.habittracker.timing.TimerBus
import com.habittracker.timing.TimerController
import com.habittracker.timing.TimerEvent
import com.habittracker.timerux.TimerCompletionInteractor
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerActionCoordinatorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val interactor: TimerCompletionInteractor = mockk()
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val timingRepository: TimingRepository = mockk(relaxed = true)
    private val timingPreferencesRepository: TimingPreferencesRepository = mockk(relaxed = true)
    private val timerController: TimerController = mockk(relaxed = true)

    private lateinit var coordinator: TimerActionCoordinator

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        coordinator = TimerActionCoordinator(
            interactor,
            habitRepository,
            timingRepository,
            timingPreferencesRepository,
            timerController,
            testScope.backgroundScope 
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `paused habits are tracked when Pause event received`() = testScope.runTest {
        val habitId = 123L
        val sessionId = 999L

        TimerBus.emit(TimerEvent.Paused(sessionId, habitId))
        advanceUntilIdle()
        delay(50) 

        val state = coordinator.state.value
        assertTrue("Paused habits should contain habit ID", state.pausedHabits.contains(habitId))
    }

    @Test
    fun `Start blocked when another habit is strictly running`() = testScope.runTest {
        // Given: Habit 1 is running
        val habit1 = 1L
        
        // Setup fallback to avoid MockKException if logic fails
        io.mockk.coEvery { interactor.decide(any(), any()) } returns TimerCompletionInteractor.ActionOutcome.Execute(emptyList())

        TimerBus.emit(TimerEvent.Started(101, habit1, 60000, false))
        advanceUntilIdle()
        delay(50)
        
        // Verify state was updated
        assertEquals("Tracked habit ID should be set", habit1, coordinator.state.value.trackedHabitId)
        assertFalse("Should not be paused", coordinator.state.value.paused)
        
        // When: Try to start Habit 2
        val decision = coordinator.decide(TimerCompletionInteractor.Intent.Start, 2L)

        // Then: Should be Disallowed
        assertTrue("Output should be Disallow, but was $decision", decision.outcome is TimerCompletionInteractor.ActionOutcome.Disallow)
        assertEquals("Finish your running habit first!", (decision.outcome as TimerCompletionInteractor.ActionOutcome.Disallow).message)
    }

    @Test
    fun `Resume blocked when another habit is strictly running`() = testScope.runTest {
        // Given: Habit 1 is running, Habit 2 is paused (in state)
        val habit1 = 1L
        val habit2 = 2L
        TimerBus.emit(TimerEvent.Started(101, habit1, 60000, false))
        TimerBus.emit(TimerEvent.Paused(102, habit2)) // Update paused list
        advanceUntilIdle()
        delay(10)

        // When: Try to Resume Habit 2
        val decision = coordinator.decide(TimerCompletionInteractor.Intent.Resume, 2L)

        // Then: Should be Disallowed
        assertTrue("Output should be Disallow", decision.outcome is TimerCompletionInteractor.ActionOutcome.Disallow)
    }

    @Test
    fun `StartBlocked when another habit is paused`() = testScope.runTest {
        // Given: Habit 1 is paused
        val habit1 = 1L
        TimerBus.emit(TimerEvent.Paused(101, habit1))
        advanceUntilIdle()
        delay(10)

        // When: Try to Start Habit 2 (New)
        val decision = coordinator.decide(TimerCompletionInteractor.Intent.Start, 2L)

        // Then: Should be Disallowed ("Finish your paused habits first!")
        assertTrue("Output should be Disallow", decision.outcome is TimerCompletionInteractor.ActionOutcome.Disallow)
        assertEquals("Finish your paused habits first!", (decision.outcome as TimerCompletionInteractor.ActionOutcome.Disallow).message)
    }

    @Test
    fun `Resume allowed for paused habit when another is paused`() = testScope.runTest {
        // Given: Habit 1 is paused
        val habit1 = 1L
        val habit2 = 2L
        TimerBus.emit(TimerEvent.Paused(101, habit1))
        TimerBus.emit(TimerEvent.Paused(102, habit2))
        advanceUntilIdle()
        delay(10)

        // When: Try to Resume Habit 2 (Existing paused)
        // Note: Coordinator.decide calls interactor.decide which is mocked.
        // We only care if it passes the blocking logic check.
        io.mockk.coEvery { interactor.decide(any(), any()) } returns TimerCompletionInteractor.ActionOutcome.Execute(listOf(TimerCompletionInteractor.Action.ResumeTimer(2L)), false)
        
        val decision = coordinator.decide(TimerCompletionInteractor.Intent.Resume, 2L)

        // Then: Should NOT be Disallow (should proceed to Execute or whatever interactor returns)
        // If interactor returns Execute, then result is Execute.
        assertFalse("Output should NOT be Disallow", decision.outcome is TimerCompletionInteractor.ActionOutcome.Disallow)
    }
}
