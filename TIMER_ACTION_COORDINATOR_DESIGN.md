# Timer Action Coordinator – Design Update

## Executive Summary

The coordinator is an application-scoped component that serialises timer actions, listens to `TimerBus`, and fans-out results to the UI via a light shared event stream. It fixes the double-start bug without introducing brittle ViewModel-to-ViewModel dependencies.

### Goals

1. **Single action gateway** – every Start/Pause/Resume/Done request flows through the coordinator.
2. **Deterministic state** – UI derives enable/disable state from `CoordinatorState` rather than guessing.
3. **Service idempotency** – `TimerService.handleStart` ignores duplicate start intents for an already running session.
4. **Analytics clarity** – one canonical telemetry stream for timer actions.

Non-goals for UIX-12:
* Smart-duration adjustments (queued for UIX-13).
* Widget integration (handled once the app surfaces are stable).

---

## Architecture Overview

```
UI (Button tap) ──► TimerActionHandler.handle(intent, habitId)
                     │
                     ▼
            TimerActionCoordinator.decide(...)
                     │
        ┌────────────┴─────────────┐
        │                          │
 TimerCompletionInteractor   TimerController / TimerService
        │                          │
        ▼                          ▼
  ActionOutcome + UiEvents    TimerBus events
        │                          │
        └────────────┬─────────────┘
                     ▼
            Coordinator state flow
                     ▼
              Compose / View layer
```

* The coordinator never reaches into other ViewModels – it talks to repositories and the service controller only.
* UI collects two flows: `state` (long-lived) and `events` (snackbars, confirms, telemetry).
* Telemetry is emitted once per accepted action; debounced taps emit a `Disallow` outcome instead of firing service calls.

---

## Key Data Structures

### CoordinatorState

```kotlin
data class CoordinatorState(
    val trackedHabitId: Long? = null,
    val timerState: TimerCompletionInteractor.TimerState = TimerCompletionInteractor.TimerState.IDLE,
    val remainingMs: Long = 0L,
    val paused: Boolean = false,
    val waitingForService: Boolean = false,
    val lastOutcome: ActionOutcome? = null
)
```

### UiEvent

```kotlin
sealed class UiEvent {
    data class Text(val message: String) : UiEvent()
    data class Confirm(val type: TimerCompletionInteractor.ConfirmType, val payload: Any?) : UiEvent()
    data class Undo(val message: String) : UiEvent()
    data class Tip(val message: String) : UiEvent()
    data class Telemetry(val intent: TimerCompletionInteractor.Intent, val outcome: ActionOutcome) : UiEvent()
}
```

Events are delivered via a cold `SharedFlow` with `extraBufferCapacity = 1` so UI surfaces can collect them without missing updates.

---

## Service Interaction

* `TimerService.handleStart` short-circuits when the target habit already owns a running session. It still resumes if the session is paused.
* `TimerService` emits ticks through `TimerBus`; the coordinator keeps a per-habit cache and updates `CoordinatorState` when the habit it tracks changes state.
* Extend/Subtract commands remain direct calls – they are additive and do not benefit from debouncing. The coordinator may eventually own them if we expose a “set target” API.

---

## UI Responsibilities

1. Inject `TimerActionHandler` through the existing Hilt graph.
2. Collect coordinator state using `collectAsStateWithLifecycle()`.
3. Disable relevant controls when `state.waitingForService` is true.
4. React to `UiEvent.Confirm` by presenting dialogs and re-posting the confirmed intent through the handler.
5. Subscribe to telemetry events in one location (e.g., `MainScreen`) and forward them to analytics.

---

## Analytics Plan

* The coordinator is the only place that calls `TrackTimerEventUseCase` for action telemetry.
* Service-level analytics remain for defensive logging but are deduplicated by session id.
* `TimingFeatureViewModel.recordTimerUsage` will be triggered from coordinator outcomes instead of button callbacks.

---

## Rollout Strategy

1. Land the coordinator behind a Gradle feature flag `timer.action.coordinator.enabled` (default off).
2. Update UI surfaces to call the handler and guard the new code behind the flag.
3. Once QA validates the flows, flip the flag in release builds and remove the legacy path.
4. After rollout, delete the fallback wiring and clean up direct controller usages.

---

## Open Items (Tracked)

| Item | Owner | Target Phase |
| ---- | ----- | ------------ |
| Implement `TimerService.ACTION_SET_TARGET` for smart duration | Runtime team | UIX-13 |
| Coordinator UI event bus sample tests | Mobile QA | UIX-12.1 |
| Widget integration | Growth | UIX-14 |

---

## Definition of Done

* Coordinator module compiles with no ViewModel dependencies.
* All interactive timer controls respect `CoordinatorState`.
* Start double-tap smoke test passes on physical device.
* Analytics dashboard shows 1:1 mapping between user intent and logged event.
* Legacy direct `TimerController.complete()` calls are removed from app surfaces.

Once these boxes are ticked, the feature is ready to move from branch to production.
