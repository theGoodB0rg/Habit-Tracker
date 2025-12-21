# Timer Coordinator: Quick Visual Reference

## Problem Overview

```
╔════════════════════════════════════════════════════════════════╗
║ Current State: BROKEN                                          ║
╠════════════════════════════════════════════════════════════════╣
║                                                                ║
║ User Perspective:                                             ║
║ ┌──────────────────────────────────────────────────────────┐ ║
║ │ Timer counting: 25:00                                    │ ║
║ │ [Start] [Pause] [Complete]                              │ ║
║ │                                                           │ ║
║ │ Action: Double-tap [Start]                              │ ║
║ │ Result: Timer FREEZES or shows wrong time                │ ║
║ │ Why: Two START commands → service re-initializes twice   │ ║
║ └──────────────────────────────────────────────────────────┘ ║
║                                                                ║
║ Developer Perspective:                                        ║
║ [UI Button]            [UI Button]       [UI Button]         ║
║     ↓                       ↓                ↓               ║
║  .start()            .start()         .complete()           ║
║     ↓                       ↓                ↓               ║
║  TimerService        TimerService     TimerService          ║
║  (no state check)     (no state check) (no guard)            ║
║                                                                ║
║ ✗ Multiple entry points                                      ║
║ ✗ No debounce                                                ║
║ ✗ No confirm prompts                                         ║
║ ✗ Analytics confused (duplicate events)                      ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝
```

---

## Solution Architecture

```
╔════════════════════════════════════════════════════════════════╗
║ New State: COORDINATOR-GATED                                  ║
╠════════════════════════════════════════════════════════════════╣
║                                                                ║
║ All UI Entry Points:                                          ║
║ ┌────────────────────────────────────────────────────────┐   ║
║ │ [Start] [Resume] [Pause] [Complete] [+5m]            │   ║
║ │ Smart Chip:                                           │   ║
║ │  └─ "10m Pomodoro?" (if timer active → dialog)        │   ║
║ └────────────┬────────────────────────────────────────────┘   ║
║              │ All routes through ONE:                        ║
║              ↓                                                ║
║        ┌─────────────────────────────┐                       ║
║        │ TimerActionCoordinator      │                       ║
║        │ (Single Source of Truth)    │                       ║
║        │                             │                       ║
║        │ ├─ Debounce: 500ms          │                       ║
║        │ ├─ In-flight tracking       │                       ║
║        │ ├─ State validation         │                       ║
║        │ └─ Route to Interactor      │                       ║
║        └────────────┬────────────────┘                       ║
║                     │                                         ║
║         ┌───────────┼───────────┐                            ║
║         ↓           ↓           ↓                            ║
║    [Execute]  [Confirm]   [Disallow]                        ║
║         │           │           │                            ║
║         ├─ Action   ├─ Dialog   ├─ Snackbar                 ║
║         │           │           │                            ║
║         ↓           ↓           ↓                            ║
║    TimerService   UI Prompt   Error Message                 ║
║    (once only)    (shows choice)(blocked)                   ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝
```

---

## Data Flow: Happy Path vs. Broken Path

### ✓ CORRECT: No Double-Start

```
User taps [Start] TWICE (50ms apart):

  TAP 1 (t=0ms)                   TAP 2 (t=50ms)
  ───────────────                 ──────────────
  coordinator.decide()            coordinator.decide()
        ↓                               ↓
  [Check: 0 < 500?]              [Check: 50 < 500?]
    NO (first tap)                  YES (debounce!)
        ↓                               ↓
  [Allow]                         [Block]
        ↓                               ↓
  TimerService                    Snackbar:
  .handleStart()                  "In flight,
        ↓                          wait"
  ONE action sent ✓              (Button disabled)
  (not two!)
  
  Result:
  ✓ Single service call
  ✓ Single event in analytics
  ✓ User sees clear feedback
```

### ✗ OLD: Double-Start Bug

```
Old code (before coordinator):

  TAP 1                           TAP 2
  ─────                           ─────
  onClick()                       onClick()
    ↓                               ↓
  timerController.start()        timerController.start()
    ↓                               ↓
  Intent(ACTION_START)           Intent(ACTION_START)
    ↓                               ↓
  TimerService                   TimerService
  .handleStart()                 .handleStart()
    ↓                               ↓
  sessionId = id₁                sessionId = id₂  ← Different!
  startMs = time₁                startMs = time₂
  targetMs = 25m                 targetMs = 25m
  emit Started(25m)              emit Started(25m) ← Resets clock!
    ↓                               ↓
  UI sees remaining = 25m        UI sees remaining = 25m
                                 (frozen at top)
                                 
  Result:
  ✗ Two sessions created
  ✗ Timer visually "freezes"
  ✗ Duplicate analytics events
```

---

## Component Interactions

```
┌─────────────────────────────────────────────────────────────────┐
│ MiniSessionBar Lifecycle (with Coordinator)                    │
└─────────────────────────────────────────────────────────────────┘

1. DISPLAY: Timer is active (state.active = true)
   ┌─────────────────────────────────────┐
   │ [⏱️ 23:45]                          │
   │ [▶ Resume] [+5m] [✕ Close] [More]  │
   └──┬──────────────────────────────┬──┘
      │ enabled based on:            │
      └─ !coordState.               │
        waitingForServiceEvent      │
                                    │
2. USER TAPS [✕ Close]:             │
   coordinator.decide(               │
     intent = Done,                  │
     habitId = session.habitId       │
   )                                 │
        ↓                            │
   Check debounce                    │
        ↓                            │
   Check time elapsed vs min         │
        ↓                            │
   TimeElapsed < MinDuration?        │
        ├─ YES → Confirm("Log partial?")
        └─ NO → Execute([Complete])  │
               │                      │
               ├─ Button disabled     │
               ├─ coord.state.        │
               │  waitingForService   │
               │  Event = true        │
               │                      │
               └─ TimerService        │
                  .handleComplete()   │
                        ↓            │
                  EmitEvent.         │
                  Completed          │
                        ↓            │
                  Coordinator        │
                  hears event        │
                        ↓            │
                  waitingForService  │
                  Event = false ← Button re-enabled
                        ↓
                  MiniSessionBar
                  disappears
```

---

## State Machine: Button Enabling

```
┌────────────────────────────────────────────┐
│ SimpleTimerButton Enabled State            │
└────────────────────────────────────────────┘

                    START (Default)
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    No timer         Timer Running     Timer Paused
    active           (not paused)      (can resume)
         │                │                │
      [Start]          [Active]        [Resume]
      enabled=✓       enabled=✗        enabled=✓
         │                │                │
         │ User taps      │                │ User taps
         ↓                │                ↓
   STARTING              │            RESUMING
   enabled=✗             │            enabled=✗
   (spinning)            │            (spinning)
         │                │                │
         │ Wait for       │                │ Wait for
         │ TimerEvent     │                │ TimerEvent
         │ .Started       │                │ .Started
         ↓                │                ↓
    RUNNING              │            RUNNING
    enabled=✗            │            enabled=✗
    (timer active)       │            (timer active)
         │                │                │
         └────────────────┴────────────────┘
                          │
                    User pauses
                    (not via coordinator)
                          │
                      PAUSED
                    enabled=✓
                   [Resume] btn
```

---

## Confirm Dialog Outcomes

```
┌──────────────────────────────────────────┐
│ Interactor.decide() → ConfirmationType   │
└──────────────────────────────────────────┘

              Intent.Done
                    │
        ┌───────────┼───────────┐
        │           │           │
    Timer?       Below       Pomodoro
    IDLE/None    Min?        Early?
        │           │           │
        │           │           │
    Execute    Confirm:     Confirm:
    Complete   BelowMin     EndPomodoro
        │           │           │
        ↓           ↓           ↓
    Mark       [Complete]  [End &
    done       [Keep]      Complete]
              [Partial]    [Keep]
              
              
┌──────────────────────────────────────────┐
│ Smart Suggestion (10m) While Active      │
└──────────────────────────────────────────┘

   User taps suggestion
   "10m Pomodoro boost"
              │
      Coordinator
      checks:
      Timer active?
          │
         YES
          │
      Show Dialog:
      ┌────────────────┐
      │ Timer active   │
      │ 15m remaining  │
      │                │
      │ [Adjust 10m]   │
      │ [Queue for     │
      │  next]         │
      │ [Cancel]       │
      └────────────────┘
```

---

## File Tree: Changes

```
app/src/main/java/com/habittracker/
│
├─ timerux/
│  ├─ TimerCompletionInteractor.kt
│  │  └─ MODIFY: Add Intent.Resume, Intent.Adjust
│  │             Add ConfirmType.ResumeInstead
│  │             Update decide() with coordinator awareness
│  │
│  └─ TimerActionCoordinator.kt ← CREATE (NEW FILE)
│     ├─ Debounce logic
│     ├─ In-flight tracking
│     ├─ State management (StateFlow)
│     └─ Event listener for service confirmation
│
├─ ui/components/
│  ├─ EnhancedHabitCard.kt
│  │  └─ MODIFY: Complete button → coordinator.decide(Done)
│  │             Show confirm dialogs
│  │             Smart suggestions: check for active timer
│  │
│  ├─ timing/
│  │  └─ TimingUIComponents.kt
│  │     └─ MODIFY: SimpleTimerButton → coordinator.decide()
│  │                Gate on remainingByHabit + coordState
│  │
│  └─ timer/
│     └─ MiniSessionBar.kt
│        └─ MODIFY: Pause/Resume/Complete → coordinator.decide()
│                   Show confirm dialogs
│                   Disable buttons while in-flight
│
└─ timing/
   ├─ TimerService.kt
   │  └─ MODIFY: handleStart() → add idempotency check
   │             Short-circuit if already running
   │
   ├─ TimerController.kt
   │  └─ No change (remains thin wrapper)
   │
   └─ TimerBus.kt, TimerEvent.kt
      └─ No change (existing)
```

---

## Key Success Metrics

### Before Implementation
- [ ] Double-taps cause timer reset? **YES (BUG)**
- [ ] Smart chips start second timer? **YES (BUG)**
- [ ] Complete while running shows prompt? **NO (BUG)**
- [ ] Analytics record duplicate start events? **YES (BUG)**
- [ ] UI ↔ Service state in sync? **NO (inconsistent)**

### After Implementation
- [ ] Double-taps debounced? **YES ✓**
- [ ] Smart chips offer dialog? **YES ✓**
- [ ] Complete shows "log partial"? **YES ✓**
- [ ] Analytics single event per action? **YES ✓**
- [ ] State always consistent? **YES ✓**

---

## Implementation Phases

```
Phase 1: Core
├─ Create TimerActionCoordinator.kt (350 lines)
├─ Update TimerCompletionInteractor.kt (50 lines)
└─ Add idempotency check to TimerService.handleStart() (10 lines)

Phase 2: First Surface (MiniSessionBar)
├─ Replace direct controller calls with coordinator.decide()
├─ Add confirm dialog handlers
├─ Test double-tap scenario
└─ **Validation: Single action per tap**

Phase 3: Start Button
├─ Gate SimpleTimerButton on remainingByHabit
├─ Show Resume/Active/Start labels
├─ Route through coordinator
└─ **Validation: No tappable while running**

Phase 4: Smart Suggestions
├─ Check for active timer before starting
├─ Show "Adjust or Queue" dialog
└─ **Validation: No second timer created**

Phase 5: Complete Button
├─ Update EnhancedHabitCard Complete callback
├─ Route through coordinator → interactor
├─ Show confirm dialogs
└─ **Validation: Below-min prompt shows**

Phase 6: Analytics
├─ Verify single event per action
├─ Remove stale recordTimerUsage() calls
└─ **Validation: Clean metrics**
```

---

## Integration Checklist

- [ ] Coordinator module builds without errors
- [ ] Hilt can inject Coordinator into ViewModels
- [ ] MiniSessionBar buttons call coordinator.decide()
- [ ] SimpleTimerButton gates on coordinator state
- [ ] Confirm dialogs render correctly
- [ ] Double-tap Start button: only ONE service call
- [ ] Double-tap Complete button: only ONE service call
- [ ] Active timer + smart chip: shows dialog, not second timer
- [ ] Below-minimum complete: confirm dialog appears
- [ ] Pomodoro early stop: confirm dialog appears
- [ ] Analytics: one event per action (check logs)
- [ ] No crashes when coordinator in-flight expires (500ms+)
- [ ] Pause/Resume buttons work while timer running
- [ ] Extend button doesn't trigger debounce error

---

## Common Pitfalls & Fixes

| Pitfall | Symptom | Fix |
|---------|---------|-----|
| `waitingForServiceEvent` never clears | Buttons stay disabled forever | Verify `Coordinator.onTimerEventReceived()` is called |
| Debounce too aggressive | User can't tap buttons | Lower `DEBOUNCE_MS` from 500 to 250 |
| Debounce too weak | Double taps still fire twice | Raise `DEBOUNCE_MS` to 750 |
| Coordinator not injected | `NullPointerException` | Add `@HiltViewModel` annotation |
| Confirm dialog doesn't show | User sees nothing after tap | Verify `is ActionOutcome.Confirm` branch executed |
| Button enabled state wrong | Button clickable while in-flight | Ensure `enabled = !coordState.waitingForServiceEvent` |
| Service still gets duplicate starts | Timer still resets | Verify idempotency check in `handleStart()` runs |

