# Timer Action Coordinator – UI Flows

This document shows how the refreshed coordinator surfaces state to the UI. Every button reflects the coordinator’s `CoordinatorState`, and all actions go through `TimerActionHandler.handle(...)` so they are debounced consistently.

---

## SimpleTimerButton State Diagram

```
Idle (no active session)
  label: "Start" / enabled when !state.waitingForService
  tap -> handler.handle(Start)
      -> Coordinator decides Start or Resume
      -> state.waitingForService = true

Starting (waiting for service)
  label: "Starting…" / disabled
  transition when TimerEvent.Started -> Running

Running (active session)
  label: "Active" / disabled
  countdown rendered via LiveRemainingTime(habitId)
  if coordinator.state.paused true -> Paused state

Paused (session paused)
  label: "Resume"
  tap -> handler.handle(Resume)
      -> disabled until TimerEvent.Resumed

Completed / Stopped -> back to Idle
```

Key rules:

* Button reads `coordState` once via `collectAsStateWithLifecycle()` and re-composes instantly.
* Debounce is handled centrally; the button simply disables itself when `waitingForService`.
* Start intent is auto-redirected to Resume if the coordinator knows the timer is running.

---

## MiniSessionBar Controls

| Control | Enabled When | Action Pipeline |
| ------- | ------------ | ---------------- |
| Pause / Resume | `!state.waitingForService` | `handler.handle(Pause or Resume)` |
| Extend +5m | `!state.waitingForService` | `timerController.extendFiveMinutes()` stays direct (non-debounced) |
| Complete | `!state.waitingForService` | `handler.handle(Done)` |
| More Sheet | Always | shows `TimerControlSheet` which also uses handler |

When `state.waitingForService` is true, all controls fade to 40% alpha with no ripple. The coordinator publishes `ActionOutcome.Confirm` events via `UiEvent.Confirm`, so the sheet can display below-minimum or partial prompts without duplicating logic.

---

## TimerControlSheet

1. Sheet collects `coordState`.
2. Primary buttons call `handler.handle`.
3. When `UiEvent.Confirm` arrives, show the dialog and only execute the confirmed branch through the handler once the user agrees.
4. Sheet closes itself after success (listens for `ActionOutcome.Execute` where the action includes `CompleteToday`).

---

## Smart Suggestion Flow (Phase UIX-12)

Current release (before Phase UIX-13 set-target support):

1. Tap suggestion while timer idle → call `handler.handle(Start)` with the suggestion’s target duration (passed in `DecisionContext.smartDuration`). The coordinator records the override but still uses the existing extend/add APIs for now.
2. Tap suggestion while timer active → show dialog:
   * **Resume current session** (default)
   * **Queue suggestion for later** (stored in local state)
   * **Cancel**

No immediate second timer is created, preserving single-session behaviour.

---

## Telemetry Hooks

The handler emits `UiEvent.Telemetry(intent, outcome)` on the main thread. Surfaces that already log analytics can remove their ad-hoc calls and instead subscribe once inside `MainScreen` or the analytics layer.

---

## Integration Checklist for Each Surface

* Inject `TimerActionHandler` (or obtain it from a shared ViewModel/provider).
* Collect `coordinator.state` and `coordinator.events`.
* Disable buttons whenever `waitingForService` is true.
* Route confirm dialogs through the shared event stream.
* Remove any direct calls to `TimerController` except for duration adjustments and stopping (which remain synchronous until the coordinator grows first-class APIs).

When all surfaces follow this contract, the user can no longer trigger double starts or silent completions.
