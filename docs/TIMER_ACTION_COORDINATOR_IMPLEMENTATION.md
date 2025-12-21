# Timer Action Coordinator – Implementation Checklist

This refreshed plan reflects the design review feedback. The coordinator lives as an application-scoped component, depends only on repository/service APIs (never on other ViewModels), and every UI entry point is routed through a single helper so we cannot forget to debounce button taps.

---

## Phase 1 – Create the Coordinator Skeleton

**File:** `app/src/main/java/com/habittracker/timerux/TimerActionCoordinator.kt`

```kotlin
package com.habittracker.timerux

import com.habittracker.data.repository.timing.TimingRepository
import com.habittracker.timing.TimerBus
import com.habittracker.timing.TimerController
import com.habittracker.timing.TimerEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerActionCoordinator @Inject constructor(
    private val interactor: TimerCompletionInteractor,
    private val timingRepo: TimingRepository,
    private val timerController: TimerController,
    @ApplicationScope private val appScope: CoroutineScope
) {
    data class CoordinatorState(
        val trackedHabitId: Long? = null,
        val timerState: TimerCompletionInteractor.TimerState = TimerCompletionInteractor.TimerState.IDLE,
        val remainingMs: Long = 0L,
        val paused: Boolean = false,
        val waitingForService: Boolean = false,
        val lastOutcome: ActionOutcome? = null
    )

    data class DecisionContext(
        val platform: TimerCompletionInteractor.Platform = TimerCompletionInteractor.Platform.APP,
        val smartDuration: Duration? = null // applied in Phase UIX-13
    )

    private val _state = MutableStateFlow(CoordinatorState())
    val state: StateFlow<CoordinatorState> = _state.asStateFlow()

    private val remainingByHabit = mutableMapOf<Long, Long>()
    private val pausedByHabit = mutableMapOf<Long, Boolean>()

    private var lastActionAt: Long = 0L
    private var inFlightIntent: TimerCompletionInteractor.Intent? = null
    private val debounceMs = 500L

    init {
        appScope.launch {
            TimerBus.events.collect(::onTimerEvent)
        }
    }

    suspend fun decide(
        intent: TimerCompletionInteractor.Intent,
        habitId: Long,
        context: DecisionContext = DecisionContext()
    ): ActionOutcome {
        val now = System.currentTimeMillis()

        if (shouldDebounce(intent, now)) {
            return ActionOutcome.Disallow("Action already in flight")
        }

        val session = timingRepo.getActiveTimerSession(habitId)
        val timing = timingRepo.getHabitTiming(habitId)
        val remaining = remainingByHabit[habitId] ?: 0L
        val isPaused = pausedByHabit[habitId] == true
        val isRunning = remaining > 0 && !isPaused

        val redirectedIntent = when {
            intent == TimerCompletionInteractor.Intent.Start && isRunning -> TimerCompletionInteractor.Intent.Resume
            intent == TimerCompletionInteractor.Intent.Start && session?.isPaused == true -> TimerCompletionInteractor.Intent.Resume
            else -> intent
        }

        val inputs = TimerCompletionInteractor.Inputs(
            habitId = habitId,
            timerEnabled = timing?.timerEnabled == true,
            requireTimerToComplete = timing?.requireTimerToComplete == true,
            minDurationSec = timing?.minDuration?.seconds?.toInt(),
            targetDurationSec = timing?.estimatedDuration?.seconds?.toInt(),
            timerState = when {
                session?.isRunning == true -> TimerCompletionInteractor.TimerState.RUNNING
                session?.isPaused == true -> TimerCompletionInteractor.TimerState.PAUSED
                else -> TimerCompletionInteractor.TimerState.IDLE
            },
            elapsedSec = session?.elapsedTime?.seconds?.toInt() ?: 0,
            todayCompleted = false,
            platform = context.platform,
            singleActiveTimer = true,
            timerType = session?.type,
            isInBreak = session?.isInBreak == true
        )

        val outcome = interactor.decide(redirectedIntent, inputs)
        if (outcome is ActionOutcome.Execute) {
            lastActionAt = now
            inFlightIntent = redirectedIntent
            _state.value = _state.value.copy(
                trackedHabitId = habitId,
                waitingForService = true,
                lastOutcome = outcome,
                timerState = inputs.timerState,
                remainingMs = remaining,
                paused = isPaused
            )
            dispatchActions(outcome.actions, habitId)
        } else {
            _state.value = _state.value.copy(lastOutcome = outcome)
        }
        return outcome
    }

    private fun shouldDebounce(intent: TimerCompletionInteractor.Intent, now: Long): Boolean {
        if (intent !in debouncedIntents) return false
        if (_state.value.waitingForService) return true
        return (now - lastActionAt) < debounceMs
    }

    private fun dispatchActions(actions: List<TimerCompletionInteractor.Action>, habitId: Long) {
        actions.forEach { action ->
            when (action) {
                is TimerCompletionInteractor.Action.StartTimer -> timerController.start(habitId)
                is TimerCompletionInteractor.Action.PauseTimer -> timerController.pause()
                is TimerCompletionInteractor.Action.ResumeTimer -> timerController.resume()
                is TimerCompletionInteractor.Action.CompleteToday -> timerController.complete()
                is TimerCompletionInteractor.Action.SavePartial -> timerController.stop()
                is TimerCompletionInteractor.Action.DiscardSession -> timerController.stop()
                is TimerCompletionInteractor.Action.ShowUndo -> emitUiEvent(UiEvent.Undo(action.message))
                is TimerCompletionInteractor.Action.ShowTip -> emitUiEvent(UiEvent.Tip(action.message))
            }
        }
    }

    private fun onTimerEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.Started -> {
                remainingByHabit[event.habitId] = event.targetMs
                pausedByHabit[event.habitId] = false
                markCompleted(event.habitId)
            }
            is TimerEvent.Tick -> remainingByHabit[event.habitId] = event.remainingMs
            is TimerEvent.Paused -> pausedByHabit[event.habitId] = true
            is TimerEvent.Resumed -> pausedByHabit[event.habitId] = false
            is TimerEvent.Completed -> {
                remainingByHabit.remove(event.habitId)
                pausedByHabit.remove(event.habitId)
                markCompleted(event.habitId)
            }
            else -> Unit
        }
    }

    private fun markCompleted(habitId: Long) {
        if (_state.value.trackedHabitId == habitId) {
            _state.value = _state.value.copy(
                waitingForService = false,
                trackedHabitId = null
            )
            inFlightIntent = null
        }
    }

    private fun emitUiEvent(event: UiEvent) {
        // Implementation detail (cold SharedFlow) added in Phase 1.1
    }

    companion object {
        private val debouncedIntents = setOf(
            TimerCompletionInteractor.Intent.Start,
            TimerCompletionInteractor.Intent.Resume,
            TimerCompletionInteractor.Intent.Pause,
            TimerCompletionInteractor.Intent.Done
        )
    }
}
```

> **Injection Support**
>
> 1. Add an `@ApplicationScope` `CoroutineScope` binding (we already have one for the analytics pipeline; reuse it).
> 2. Provide `TimerController` through Hilt so that the coordinator can receive it:
>
> ```kotlin
> @Module
> @InstallIn(SingletonComponent::class)
> object TimingControllerModule {
>     @Provides
>     @Singleton
>     fun provideTimerController(@ApplicationContext context: Context) = TimerController(context)
> }
> ```
>
> 3. No other ViewModel should be injected – the coordinator listens to `TimerBus` and repositories directly.

---

## Phase 2 – Coordinator Helper for UI

To avoid forgetting a surface, create a reusable helper in `timerux`:

```kotlin
class TimerActionHandler @Inject constructor(
    private val coordinator: TimerActionCoordinator,
    @MainDispatcher private val mainScope: CoroutineScope
) {
    fun handle(intent: TimerCompletionInteractor.Intent, habitId: Long) {
        mainScope.launch {
            val outcome = coordinator.decide(intent, habitId)
            coordinator.handleOutcome(outcome, habitId)
        }
    }
}
```

Any composable or presenter calls `handler.handle(...)`; buttons read `coordinator.state` to disable themselves when `waitingForService` is true.

---

## Phase 3 – Touch Points to Update

| Surface / File | Action |
| -------------- | ------ |
| `SimpleTimerButton` (`TimingUIComponents.kt`) | Inject `TimerActionHandler` via `hiltViewModel()` owner, read `coordState` from `TimerActionCoordinator`, disable button when `waitingForService`, swap label to "Resume" and "Active" states. |
| `MiniSessionBar` | Replace direct `TimerController` calls with `handler.handle(...)`, and hide buttons while coordinator reports in-flight actions. |
| `TimerControlSheet` | Route `Complete` through the coordinator; show dialogs based on `ActionOutcome.Confirm`. |
| `EnhancedHabitCard` | Use coordinator for the quick actions, and gate smart suggestions (display "Adjust/Queue" dialog). For now, dismiss the dialog if the user chooses "Adjust" until Phase UIX-13 implements smart-duration support. |
| Widgets / Notifications | Future work; coordinator exposes a `resumeSession` helper once the app surface is stable. |

Add a shared composable extension:

```kotlin
fun Modifier.disableDuringTimerAction(state: TimerActionCoordinator.CoordinatorState) =
    if (state.waitingForService) this.then(Modifier.alpha(0.4f)) else this
```

Use it on every timer control to ensure consistent visual feedback.

---

## Phase 4 – TimerService Adjustments

1. **Idempotent start:** check `session?.isRunning == true` and return early before rebuilding state.
2. **Broadcast tick ownership:** emit `TimerEvent.Started` only once per unique session id.
3. **Extend API:** expose `TimerService.ACTION_SET_TARGET` in Phase UIX-13; for now we leave smart duration adjustments as "queued". Update docs to reflect this staging.

---

## Phase 5 – Analytics Wiring

* Coordinator emits `TimerActionTelemetry` events (start, pause, resume, done, debounced). Wire this to `TrackTimerEventUseCase` so that analytics sees a single canonical stream.
* Remove legacy `recordTimerUsage` calls from `EnhancedHabitCard` (they will move to the coordinator once we confirm the DSP story).
* Keep service-side analytics for sanity checks, but add guards so duplicate calls are ignored when the same session id is passed twice.

---

## Regression Test Matrix

| Scenario | Expected Behaviour |
| -------- | ------------------ |
| Double-tap Start | Second tap receives `ActionOutcome.Disallow`, snackbar shows message, no second `ACTION_START`. |
| Start -> Pause within 100 ms | Pause is ignored until `TimerEvent.Started` arrives. |
| Complete below minimum | Coordinator surfaces `Confirm(BelowMinDuration)`, UI renders dialog, only one completion is sent after confirmation. |
| Smart suggestion while running | Coordinator blocks immediate start, UI shows dialog with "Adjust / Queue / Cancel". |
| Resume after auto pause | Start chip shows "Resume", tap routes through coordinator, service receives only `ACTION_RESUME`. |

---

## Rollback Plan

If the coordinator causes regressions:

1. Revert call sites to use `TimerController` directly (keep helper in place for quick re-enable).
2. Disable the idempotency guard in `TimerService` (new code is commented so it can be toggled).
3. Turn off `TimerActionTelemetry` by guarding the Hilt binding with a feature flag.

The existing tests continue to pass because the coordinator is additive until we flip the feature flag in Phase UIX-12.

---

## Ready for Production

The code is considered production-ready when:

* All start/pause/resume/complete entry points call `TimerActionHandler.handle`.
* Coordinator state drives button enabled states in `SimpleTimerButton`, `MiniSessionBar`, and `TimerControlSheet`.
* `TimerService.handleStart` returns early for already running sessions.
* Analytics show one event per user action in back-to-back interaction tests.
* Smart suggestion "Adjust" button is hidden until Phase UIX-13 delivers the service support.

Document these acceptance criteria in the release checklist before merging the coordinator branch. 
