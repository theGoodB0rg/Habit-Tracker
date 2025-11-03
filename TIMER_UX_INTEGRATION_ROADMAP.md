# Timer-Linked Completion UX: Integration Roadmap (Phase 1 and beyond)

This document defines how habit selection, timing, and completion (“Done”) work together with clear, low-friction UX. It also covers confirmation rules, data model updates, implementation phases, testing, and rollout.

## Goals
- Keep completion fast and natural; never force a timer.
- Make timers optional, powerful, and recoverable.
- Provide smart confirmations that protect users without blocking flow.
- Ensure consistent behavior in app, widget, and notifications.

## Core principles
- Undo-first: prefer non-blocking Undo over modal confirms.
- Intent-aware: elevate confirmation only when it’s actually risky.
- Single source of truth: central interactor decides outcomes; UI obeys.
- Robust timing: persist events; compute elapsed from monotonic time.
- Accessibility and clarity: readable labels, large targets, screen-reader hints.

---

## Interaction model (Habit card + sheet)
- Habit card primary action adapts by state:
  - No timer: “Mark done”.
  - Timer idle: “Start”.
  - Running: “Pause” (with secondary “Done”).
  - Paused: “Resume” (with secondary “Done”).
- Tap behavior:
  - No timer: mark done immediately with Undo.
  - Timer: open a bottom sheet with live timer; optional “start immediately on tap” setting.
- Long-press:
  - Quick “Complete now” with Undo (bypasses timer regardless of mode).
- Bottom sheet includes:
  - Time readout + circular progress.
  - Start/Pause/Resume, Done, +/- 1 min, Add note, “Log partial”.
  - Optional toggle: “Auto-complete at target”.

## Timer modes (per habit)
- OFF: simple checkbox habits.
- COUNTDOWN: target duration with optional auto-complete at target.
- STOPWATCH: open-ended; manual completion.
- POMODORO: intervals (e.g., 25/5) with auto-advance and per-interval credit.

## Confirmation and guard-rails
Use Undo by default; escalate to modal only when needed.

- No timer (OFF)
  - Mark done: No modal. Show Undo snackbar.
- Timer configured but idle (not started today)
  - Mark done: No modal. Show Undo. First-time tip: “You can time this habit.”
  - If requireTimerToComplete=true: Disallow; show inline message “Start timer to complete.”
- Running/Paused, small progress (<20% of target or <2m for stopwatch)
  - Press Done: No modal. Show Undo.
  - If minDurationSec set and not met: Modal confirm with options:
    - Primary: “Complete anyway”.
    - Secondary: “Log partial”.
    - Tertiary: “Keep timing”.
- Running/Paused, meaningful progress (≥20% target or ≥2m)
  - Press Done: No modal. Show Undo. Log duration.
  - If autoCompleteOnTarget=true and <90% to target: Show gentle note “2m left to auto-complete” (no modal).
- Stopping a running timer without completion (discarding non-zero)
  - If elapsed>0 and action is Discard: Modal confirm “Discard 7m session?”
  - Offer “Save as partial” alternative.
- Switching timers (single-active mode)
  - Auto-pause current, start new; show snackbar “Paused X — Resume”. No modal.
- Pomodoro specifics
  - End focus early: Modal confirm “End focus early?” (End & Complete / Keep focus).
  - Skip break or reset cycle: Modal confirm.
- Widget/notification actions
  - Avoid modals. Use Undo (snackbar or notification action) and updated notification state.

### Copy snippets
- Complete: “Marked as done. Undo”
- Skip timer: “Completed without timing. Undo”
- Min threshold: “Below your minimum Xm. Complete anyway or log as partial?”
- Discard session: “Discard 7m session?”
- Require timer: “Start timer to complete this habit.”
- Near target: “About 2m to reach your target.”

---

## Data model additions (Room/DB)
- Habit
  - timerMode: OFF | COUNTDOWN | STOPWATCH | POMODORO (default OFF)
  - targetDurationSec: Int? (nullable, for COUNTDOWN/POMODORO)
  - minDurationSec: Int? (optional minimum to count as done)
  - autoCompleteOnTarget: Boolean (default false)
  - requireTimerToComplete: Boolean (default false)
- Session
  - id, habitId, startTs, endTs?, durationSec?, state: RUNNING|PAUSED|ENDED
  - source: MANUAL|AUTO|WIDGET|NOTIF
- DailyCompletion (or derive from Sessions)
  - date, habitId, completed: Boolean, totalDurationSec, sessionsCount

Migration: default existing habits to timerMode=OFF; completion unchanged.

---

## Interactor contract (central decision-maker)
Single, testable entry-point that turns an intent into an outcome the UI can enact.

- Inputs
  - habit: Habit
  - uiState: { timerState: IDLE|RUNNING|PAUSED, elapsedSec, todayCompleted }
  - context: { platform: APP|WIDGET|NOTIF, singleActiveTimer: Boolean }
  - intent: START | PAUSE | RESUME | DONE | STOP_WITHOUT_COMPLETE | QUICK_COMPLETE
- Output
  - ActionOutcome
    - Execute(actions: List<Action>, undoable: Boolean)
    - Confirm(type: ConfirmType, payload)
    - Disallow(message)
- Actions (examples)
  - StartTimer(habitId), PauseTimer, ResumeTimer, CompleteToday(logDuration: Boolean, partial: Boolean), SavePartial(duration), DiscardSession, ShowUndo, ShowTip
- ConfirmType
  - BelowMinDuration
  - DiscardNonZeroSession
  - EndPomodoroEarly

This isolates rules and enables consistent behavior across app, widget, and notifications.

---

## State machine (per habit)
- Idle -> Running (Start)
- Running -> Paused (Pause)
- Paused -> Running (Resume)
- Running/Paused -> Completed (Done or AutoComplete)
- Any -> Idle (Discard if zero, else confirm)
- Crash recovery: reconstruct from persisted timestamps; resume Running/Paused.

---

## Services, notifications, widget
- ForegroundService for active timer, ongoing notification with actions: Pause/Resume/Done.
- Single ActiveTimerCoordinator enforces one-active rule when enabled.
- Widget buttons route intents through the interactor; widget shows Start/Pause/Resume/Done depending on state.
- Throttle widget refresh; avoid per-second updates.

---

## Settings
- Per-habit
  - Uses timer (mode), target/min, auto-complete.
  - requireTimerToComplete (default false).
  - askBeforeSkippingTimer (optional; default false) — shows a one-time confirm when Done without timer.
- Global
  - Single active timer (default on).
  - Tap behavior: open sheet vs start immediately.
  - Haptics on start/pause/done (toggle).

---

## Accessibility & polish
- Large touch targets (48dp+), readable labels, color contrast.
- Content descriptions include elapsed/remaining.
- Haptics: light tick on start/pause/done (optional).
- Avoid modal stacking; ensure focus management in sheets/dialogs.

---

## Analytics and export (offline/local)
- Log: start, pause, resume, done, auto-complete, discard, partial save.
- Export sessions and daily completion with sources.
- No network telemetry unless explicitly enabled elsewhere.

---

## Edge cases
- App killed while timing: recover via persisted startTs/pause offsets.
- Device time changes: use elapsedRealtime() deltas when computing.
- Multiple timers when single-active: auto-pause old with snackbar.
- Low battery/doze: ForegroundService keeps timing accurate; recompute on resume.
- Negative or zero durations guarded in interactor.

---

## Phase plan and acceptance criteria

### Phase 1 — Foundations (Data + Interactor + Simple UI wiring)
- Implement data fields and Room migration with safe defaults.
- Build Interactor with intents DONE/START/PAUSE/RESUME/STOP_WITHOUT_COMPLETE/QUICK_COMPLETE.
- Wire one Habit card in Compose to use the Interactor and show Undo snackbar.
- Implement thresholds: minDurationSec and 20%/2m heuristic.
- Add first-time tip for skipping timer.
- Acceptance
  - Habits with timer OFF complete with single tap + Undo.
  - Habits with timer ON can start/pause/resume; Done records duration without modal in normal cases.
  - Below min threshold triggers confirm dialog.
  - Discarding non-zero session triggers confirm.

### Phase 2 — Bottom sheet + Countdown/Stopwatch polish
- Full sheet UI: progress ring, controls, +/-1m, Log partial.
- Auto-complete on reaching target (optional per habit).
- Single-active-timer coordinator.
- Acceptance
  - Sheet reflects states accurately; auto-complete works with gentle nudge before target.

### Phase 3 — ForegroundService + Notification actions
- Ongoing notification with Pause/Resume/Done; mirrors interactor.
- Robust process death recovery.
- Acceptance
  - Timing continues across background; actions reliable; state restored after kill.

### Phase 4 — Widget actions
- Widget buttons map to intents; Undo via snackbar or notification action.
- Acceptance
  - Start/Pause/Resume/Done from widget consistent with in-app behavior.

### Phase 5 — Pomodoro mode
- Add intervals, auto-advance, and early-end confirmation.
- Acceptance
  - Pomodoro cycles stable; early end asks; summaries accurate.

### Phase 6 — Settings + Accessibility pass
- Per-habit settings UI; global toggles; haptics option.
- A11y review, labels, and focus order fixes.
- Acceptance
  - Configurable behavior persists and is respected; a11y checks pass.

### Phase 7 — Export + Stabilization
- Update export to include sessions; add local analytics events.
- Fix bugs, performance, and battery usage.
- Acceptance
  - Export matches schema; no regressions in daily completion.

---

## QA and testing checklist
- Unit tests: interactor outcomes for each intent and edge case.
- Integration: service/notification actions, crash recovery.
- UI tests: habit card states; bottom sheet; dialogs; Undo flow.
- Widget tests: intent routing and state reflection.
- Performance: no jank in timer updates; limited recompositions.

---

## Implementation notes (Kotlin/Compose)
- Compute elapsed with SystemClock.elapsedRealtime() deltas; store startTs (wall) and startElapsed (monotonic) to recompute.
- Persist events immediately; recompute derived values on resume.
- Use StateFlow for HabitUiState; avoid per-second timers in composables (derive from VM).
- DI: Interactor injected into ViewModels and receivers/services.

---

## Risks and mitigations
- Too many dialogs → Undo-first, modal only for destructive/violations.
- Timer drift → monotonic deltas + recompute at each state change.
- Widget inconsistencies → single interactor shared across entry points.
- Discoverability of long-press → short tool-tip on first use.

---

## Rollout and metrics (local)
- Track usage of Done with/without timer, confirmations shown, dismiss vs proceed.
- Surface a subtle nudge if user often skips timers: “Prefer quick check-off? Turn off timer for this habit.”

---

## Appendix: minimal interactor sketch (non-binding)

```kotlin
sealed interface Intent { object Start; object Pause; object Resume; object Done; object StopWithoutComplete; object QuickComplete }

sealed interface ActionOutcome {
  data class Execute(val actions: List<Action>, val undoable: Boolean = true) : ActionOutcome
  data class Confirm(val type: ConfirmType, val payload: Any? = null) : ActionOutcome
  data class Disallow(val message: String) : ActionOutcome
}

sealed interface Action {
  data class StartTimer(val habitId: Long): Action
  data class PauseTimer(val habitId: Long): Action
  data class CompleteToday(val habitId: Long, val logDuration: Boolean, val partial: Boolean = false): Action
  data class SavePartial(val habitId: Long, val durationSec: Int): Action
  data class DiscardSession(val habitId: Long): Action
  data class ShowUndo(val message: String): Action
}

enum class ConfirmType { BelowMinDuration, DiscardNonZeroSession, EndPomodoroEarly }
```

This contract lets UI surfaces be dumb and consistent while business rules live in one place.

---

## Next steps
- Implement Phase 1: DB migration + Interactor + wire a single habit card to validate the flow end-to-end.
- After validation, expand to all habit cards, then proceed to Phase 2.
