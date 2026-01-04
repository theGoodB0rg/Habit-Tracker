# Timer State Simplification Plan

> **Living Document** — Update this file as phases are completed. Each phase must be validated with `./gradlew assembleDebug` before marking complete and committing.

**Created:** January 3, 2026  
**Status:** 🟡 In Progress  
**Goal:** Consolidate fragmented timer state management into a single source of truth, eliminating logic flow bugs and race conditions.

---

## ⚠️ Implementation Strategy: NEW UI Files

> **CRITICAL:** We are building **new UI files** that follow the design spec in [habit-card-redesign.html](ui-mockup/habit-card-redesign.html).
> 
> The old UI code (`MainScreen.kt`, `EnhancedHabitCard.kt`) stays in place but is gated behind a feature flag. This approach is faster and safer — we don't fight 2000+ lines of legacy code.

### Reference Design
**See:** [habit-card-redesign.html](ui-mockup/habit-card-redesign.html) — Contains:
- Visual mockups for all states (default, timer active, switch sheet, confirmation, loading, error)
- Complete decision matrix for all user actions
- State architecture diagram
- Responsive behavior specs
- Implementation phases

---

## Table of Contents
1. [Problem Statement](#problem-statement)
2. [Current Architecture (Fragmented)](#current-architecture-fragmented)
3. [Target Architecture (Unified)](#target-architecture-unified)
4. [Decision Matrix](#decision-matrix)
5. [Phase 1: State Consolidation](#phase-1-state-consolidation)
6. [Phase 2: UI Simplification](#phase-2-ui-simplification)
7. [Phase 3: Polish & Edge Cases](#phase-3-polish--edge-cases)
8. [Validation Checklist](#validation-checklist)
9. [Commit Log](#commit-log)

---

## Problem Statement

The home screen timer functionality has accumulated multiple sources of truth, leading to:
- **Logic flow bugs** — UI doesn't always reflect actual timer state
- **Race conditions** — Timer switching between habits causes inconsistent behavior
- **Slow progress fixing bugs** — Each fix introduces new edge cases due to fragmentation

### Symptoms
- Timer state sometimes doesn't update when switching habits
- Completion button behavior inconsistent with timer state
- Multiple confirmation dialogs can stack
- State sync issues between UI components

---

## Current Architecture (Fragmented)

### State Sources (5+ competing sources)
| Source | Location | What It Tracks |
|--------|----------|----------------|
| `TimerActionCoordinator` | [TimerActionCoordinator.kt](../app/src/main/java/com/habittracker/timerux/TimerActionCoordinator.kt) | trackedHabitId, timerState, remainingMs, paused, waitingForService |
| `TimerTickerViewModel` | [TimerTickerViewModel.kt](../app/src/main/java/com/habittracker/ui/viewmodels/timing/TimerTickerViewModel.kt) | remainingByHabit map, pausedByHabit map |
| `ActiveTimerViewModel` | [ActiveTimerViewModel.kt](../app/src/main/java/com/habittracker/ui/viewmodels/timing/ActiveTimerViewModel.kt) | active, sessionId, habitId, totalMs, remainingMs, paused |
| `PartialSessionViewModel` | [PartialSessionViewModel.kt](../app/src/main/java/com/habittracker/ui/viewmodels/timing/PartialSessionViewModel.kt) | partial session data |
| `TimerBus` | [TimerBus.kt](../app/src/main/java/com/habittracker/timing/TimerBus.kt) | Raw timer events (Started, Tick, Paused, Resumed, Completed, etc.) |

### Key UI Files
| File | Lines | Purpose |
|------|-------|---------|
| [MainScreen.kt](../app/src/main/java/com/habittracker/ui/screens/MainScreen.kt) | ~1009 | Home screen with habit list, multiple LaunchedEffects collecting from different sources |
| [EnhancedHabitCard.kt](../app/src/main/java/com/habittracker/ui/components/EnhancedHabitCard.kt) | ~1293 | Complex habit card with timer controls, 4+ confirmation dialogs |
| [TimerSwitcherSheet.kt](../app/src/main/java/com/habittracker/ui/components/timer/TimerSwitcherSheet.kt) | ~260 | Bottom sheet for timer switching |
| [MiniSessionBar.kt](../app/src/main/java/com/habittracker/ui/components/timer/MiniSessionBar.kt) | - | Global floating timer bar |

### Core Logic Files
| File | Purpose |
|------|---------|
| [TimerCompletionInteractor.kt](../app/src/main/java/com/habittracker/timerux/TimerCompletionInteractor.kt) | Decision engine: Intent → ActionOutcome |
| [TimerActionHandler.kt](../app/src/main/java/com/habittracker/timerux/TimerActionHandler.kt) | Entry point for UI → Coordinator |
| [TimerController.kt](../app/src/main/java/com/habittracker/timing/TimerController.kt) | Service communication layer |
| [TimerService.kt](../app/src/main/java/com/habittracker/timing/TimerService.kt) | Foreground service for timer |

---

## Target Architecture (Unified)

### Single Source of Truth: `TimerActionCoordinator.state`

```kotlin
data class CoordinatorState(
    // Active timer
    val activeHabitId: Long? = null,
    val timerState: TimerState = TimerState.IDLE,
    val remainingMs: Long = 0L,
    val targetMs: Long = 0L,
    val paused: Boolean = false,
    
    // Loading/error
    val isLoading: Boolean = false,
    val lastError: String? = null,
    
    // Auto-paused timer (for switch sheet)
    val pausedHabitId: Long? = null,
    val pausedRemainingMs: Long = 0L,
    
    // Confirmation tracking
    val pendingConfirmHabitId: Long? = null,
    val pendingConfirmType: ConfirmType? = null
)

enum class TimerState { IDLE, RUNNING, PAUSED, AT_TARGET }
```

### Data Flow (Simplified)
```
User Action → TimerActionHandler.handle()
                    ↓
           TimerActionCoordinator.decide()
                    ↓
        ┌──────────┴──────────┐
        ↓                     ↓
   TimerService          UI Events
   (via Controller)      (Snackbar, Confirm, etc.)
        ↓                     ↓
   TimerBus.emit()      _events.emit()
        ↓                     ↓
   Coordinator.onTimerEvent()  UI.collect()
        ↓
   _state.update()
        ↓
   ALL UI reads from state
```

---

## Decision Matrix

### Scenario: Mark Complete While Timer Running (Same Habit)

| Condition | Action | UI Feedback |
|-----------|--------|-------------|
| Timer ≥ target duration | **Execute:** Complete + stop + log duration | ✅ "Completed! Duration logged" + Undo |
| Timer ≥ min duration | **Execute:** Complete + stop + log duration | ✅ "Completed early. Duration logged" + Undo |
| Timer < min duration | **Confirm:** Show dialog | ⚠️ "Only X min completed. Complete anyway?" |
| Pomodoro focus segment | **Confirm:** Show dialog | ⚠️ "End focus session early?" |

### Scenario: Start Timer While Another Running

| Condition | Action | UI Feedback |
|-----------|--------|-------------|
| Single-active mode ON | **Execute:** Auto-pause old + start new | Bottom sheet showing both timers |
| Same habit (paused) | **Execute:** Resume | Timer resumes |
| Same habit (running) | **No-op** | No change |

### Scenario: Mark Complete (No Timer Running)

| Condition | Action | UI Feedback |
|-----------|--------|-------------|
| Timer NOT required | **Execute:** Complete immediately | ✅ "Marked as done" + Undo |
| Timer required | **Disallow:** Show message | ❌ "Start timer first to complete" |
| Timer enabled but not required | **Confirm:** Ask user | ⚠️ "Complete without timing?" |

---

## Phase 1: Coordinator Enhancement + Feature Flag

**Status:** ✅ Complete  
**Completed:** January 4, 2026  
**Validation:** `./gradlew assembleDebug` PASSED

> **Note:** This phase enhances the coordinator (backend) but does NOT touch existing UI files.

### Tasks

#### 1.1 Enhance CoordinatorState ✅
**File:** [TimerActionCoordinator.kt](../app/src/main/java/com/habittracker/timerux/TimerActionCoordinator.kt)

- [x] Add `targetMs: Long` to track total duration
- [x] Add `pausedHabitId: Long?` for auto-paused timer tracking
- [x] Add `pausedRemainingMs: Long` for auto-paused timer display
- [x] Add `lastError: String?` for error state display
- [x] Rename `waitingForService` → `isLoading` for clarity (with deprecated alias for compatibility)
- [x] Add `clearPausedHabit()` and `clearError()` methods

#### 1.2 Consolidate TimerBus Event Handling ✅
**File:** [TimerActionCoordinator.kt](../app/src/main/java/com/habittracker/timerux/TimerActionCoordinator.kt)

- [x] Handle `TimerEvent.AutoPaused` → update `pausedHabitId`, `pausedRemainingMs`
- [x] Handle `TimerEvent.Started` → update `targetMs` from event
- [x] Handle `TimerEvent.Extended` → update `targetMs` when timer extended
- [x] Add `clearPausedHabit()` function to clear when user dismisses switch sheet or resumes

#### 1.3 Add Feature Flag for New UI ✅
**File:** [TimerFeatureFlags.kt](../app/src/main/java/com/habittracker/timing/TimerFeatureFlags.kt)

- [x] Add `useSimplifiedHomeScreen: Boolean` flag
- [x] Wire flag in navigation (prepared for SimpleMainScreen in Phase 2)

### Phase 1 Validation
```powershell
./gradlew assembleDebug  # PASSED January 4, 2026
```

### Phase 1 Commit
```
git add -A
git commit -m "Phase 1: Consolidate timer state into TimerActionCoordinator

- Add targetMs, pausedHabitId, pausedRemainingMs, lastError to CoordinatorState
- Rename waitingForService → isLoading (with deprecated alias)
- Handle TimerEvent.AutoPaused to track auto-paused timers
- Handle TimerEvent.Started/Extended to set targetMs
- Add clearPausedHabit() and clearError() methods
- Prepare navigation for SimpleMainScreen feature flag

Validated: ./gradlew assembleDebug PASSED"
```

---

## Phase 2: Build New UI (Following HTML Spec)

**Status:** 🔴 Not Started  
**Estimated:** 2-3 days  
**Depends on:** Phase 1 Complete  
**Design Reference:** [habit-card-redesign.html](ui-mockup/habit-card-redesign.html)

### New File Structure

```
app/src/main/java/com/habittracker/ui/
├── screens/
│   └── simple/
│       └── SimpleMainScreen.kt          # New home screen
├── components/
│   └── simple/
│       ├── SimpleHabitCard.kt           # New habit card
│       ├── HabitTimerSection.kt         # Collapsible timer section
│       ├── SimpleMiniSessionBar.kt      # Global floating timer bar
│       └── SimpleTimerSwitcherSheet.kt  # Timer switch bottom sheet
```

### Tasks

#### 2.1 Create SimpleHabitCard (Per HTML Mockup)
**New File:** `app/src/main/java/com/habittracker/ui/components/simple/SimpleHabitCard.kt`

**Layout from mockup:**
```
┌─────────────────────────────────────────┐
│ [habit-info]                  [action-btn] │
│  ├─ habit-name (+ timer indicator if active)│
│  └─ habit-meta (streak, frequency)        │
├─────────────────────────────────────────┤
│ [timer-section] (collapsible)              │
│  └─ Collapsed: timer-chip "Start 25m"      │
│  └─ Expanded: timer-time + controls + bar  │
└─────────────────────────────────────────┘
```

- [ ] Card states: default, completed (primary-container bg), timer-active (border)
- [ ] Action button: empty circle → checkmark when completed
- [ ] Timer Required badge: show when habit requires timer
- [ ] Timer chip: "Start Xm" when collapsed
- [ ] Timer expanded: large time display + pause/complete buttons + progress bar
- [ ] Pulsing indicator dot when timer active
- [ ] Target: ~200-300 lines

#### 2.2 Create HabitTimerSection
**New File:** `app/src/main/java/com/habittracker/ui/components/simple/HabitTimerSection.kt`

- [ ] Collapsed state: AssistChip with timer icon + "Start Xm"
- [ ] Expanded state: 
  - Large time display (32sp, tabular-nums)
  - Pause button (secondary container)
  - Complete button (primary)
  - Progress bar (4dp height)
  - "X:XX elapsed / Target: XX:XX" labels
- [ ] Auto-expand when timer running for this habit
- [ ] Props: `isActive`, `remainingMs`, `targetMs`, `paused`, `onStart`, `onPause`, `onResume`, `onComplete`

#### 2.3 Create SimpleMainScreen
**New File:** `app/src/main/java/com/habittracker/ui/screens/simple/SimpleMainScreen.kt`

- [ ] Single `coordinatorState` collection
- [ ] App bar: "My Habits" title + "Today • Jan 3, 2026" subtitle
- [ ] LazyColumn/LazyVerticalGrid based on screen size
- [ ] Pass coordinator state to each SimpleHabitCard
- [ ] Error banner at top when `coordinatorState.lastError` present
- [ ] Loading skeleton when habits loading
- [ ] Target: ~300-400 lines

#### 2.4 Create SimpleMiniSessionBar (Per HTML Mockup)
**New File:** `app/src/main/java/com/habittracker/ui/components/simple/SimpleMiniSessionBar.kt`

**Layout:**
```
┌──────────────────────────────────────────┐
│ [icon] [time]  [habit-name]    [⏸] [✓] │
│         20:42   Morning Med              │
└──────────────────────────────────────────┘
```

- [ ] Fixed position above bottom nav (80dp from bottom)
- [ ] Primary background, white text
- [ ] Large time (20sp), small habit name (13sp)
- [ ] Pause + Complete buttons (36x36dp)
- [ ] Only visible when `coordinatorState.activeHabitId != null`
- [ ] Stateless: all data from props

#### 2.5 Create SimpleTimerSwitcherSheet (Per HTML Mockup)
**New File:** `app/src/main/java/com/habittracker/ui/components/simple/SimpleTimerSwitcherSheet.kt`

**Layout:**
```
┌──────────────────────────────────────────┐
│ 🔄 Timer Switched                         │
│ Started new timer. Previous paused.       │
├──────────────────────────────────────────┤
│ NOW ACTIVE (primary-container)            │
│   Deep Work                               │
│   45:00 remaining        [●] (pulsing)    │
├──────────────────────────────────────────┤
│ PAUSED (surface-variant)                  │
│   Morning Meditation                      │
│   18:42 remaining       [Resume This]     │
├──────────────────────────────────────────┤
│          [Got it] (filled button)         │
└──────────────────────────────────────────┘
```

- [ ] Reads from `coordinatorState.pausedHabitId`, `coordinatorState.pausedRemainingMs`
- [ ] "Resume This" pauses current, resumes paused
- [ ] "Got it" dismisses sheet, clears pausedHabitId
- [ ] Stateless: all data from props

#### 2.6 Wire Feature Flag
**File:** Update navigation + TimerFeatureFlags

- [ ] Add `useSimplifiedHomeScreen: Boolean = false` to feature flags
- [ ] Navigation: when true → `SimpleMainScreen`, else → `MainScreen`
- [ ] Keep old screens intact for rollback

### Phase 2 Validation
```powershell
./gradlew clean assembleDebug
# Toggle feature flag and test both UIs work
```

---

## Phase 3: Polish & Edge Cases

**Status:** 🔴 Not Started  
**Estimated:** 1 day  
**Depends on:** Phase 2 Complete

> **Note:** These tasks apply to the NEW SimpleMainScreen/SimpleHabitCard only.

### Tasks

#### 3.1 Loading States
- [ ] Show spinner on action button when `coordinatorState.isLoading`
- [ ] Disable all timer controls during loading

#### 3.2 Error Handling
- [ ] Display `coordinatorState.lastError` as dismissible banner
- [ ] Auto-clear error after 5 seconds

#### 3.3 Edge Case Testing
- [ ] Test: Rapid clicking complete button
- [ ] Test: Start timer on Habit B while A running → switch sheet appears
- [ ] Test: Resume paused timer from switch sheet
- [ ] Test: Complete habit while below min duration → confirmation dialog
- [ ] Test: App backgrounded during timer → state persists on return
- [ ] Test: Rotation during timer → state preserved

#### 3.4 Deprecate Unused ViewModels
- [ ] Add `@Deprecated` annotation to `TimerTickerViewModel`
- [ ] Add `@Deprecated` annotation to `ActiveTimerViewModel`
- [ ] TODO comment: Remove in next major version

### Phase 3 Validation
```powershell
./gradlew clean assembleDebug
./gradlew :app:testDebugUnitTest  # If tests exist
```

### Phase 3 Commit
```
git add -A
git commit -m "Phase 3: Polish and edge case handling

- Add loading skeleton and button spinners
- Add error banner with auto-dismiss and retry
- Deprecate TimerTickerViewModel, ActiveTimerViewModel
- Manual testing of all edge cases passed

Validated: ./gradlew assembleDebug PASSED"
```

---

## Validation Checklist

### Build Validation
- [x] `./gradlew clean assembleDebug` passes *(Baseline validated Jan 3, 2026)*
- [ ] No new lint errors introduced
- [ ] No new compiler warnings

### Functional Validation
| Test Case | Expected | Actual | Pass? |
|-----------|----------|--------|-------|
| Start timer on habit | Timer starts, UI updates | | |
| Pause timer | Timer pauses, pause icon shown | | |
| Resume timer | Timer resumes from paused time | | |
| Complete with timer running | Stops timer, logs duration, marks complete | | |
| Complete below min duration | Shows confirmation dialog | | |
| Start timer while another running | Auto-pauses old, shows switch sheet | | |
| Resume from switch sheet | Pauses new, resumes old | | |
| Complete without timer (not required) | Completes immediately | | |
| Complete without timer (required) | Shows "start timer first" message | | |
| Rapid click complete button | Only one completion, no duplicates | | |
| App background during timer | Timer continues in service | | |
| Return to app | UI shows correct timer state | | |

---

## Commit Log

| Date | Phase | Commit Hash | Message |
|------|-------|-------------|---------|
| Jan 3, 2026 | Baseline | `7d49157` | Baseline build fixes: enable core library desugaring, fix duplicate resources |
| - | 1 | - | Pending |
| - | 2 | - | Pending |
| - | 3 | - | Pending |

---

## Related Documents
- [UI Mockup (HTML)](./ui-mockup/habit-card-redesign.html) — Visual design reference
- [Timer Action Coordinator Design](./TIMER_ACTION_COORDINATOR_DESIGN.md) — Original coordinator architecture
- [Timer UI Flow Fix Plan](./TIMER_UI_FLOW_FIX_PLAN.md) — Previous bug fixes (Phases 1-3 complete)
- [Issues Worklog](./issues.md) — Timer coordinator task tracking

---

## Notes & Decisions

### Why Not Full Rebuild?
- Coordinator architecture is sound — reuse it
- Data layer (entities, DAOs, repositories) is solid
- Full rebuild risks losing battle-tested business logic
- Incremental simplification is lower risk

### Why Remove Multiple ViewModels?
- `TimerTickerViewModel` duplicates what coordinator already tracks
- `ActiveTimerViewModel` duplicates what coordinator already tracks
- Multiple sources → sync bugs → logic flow issues
- Single source of truth → predictable behavior

### Feature Flags
- `TimerFeatureFlags.enableActionCoordinator` is already `true` by default
- No new flags needed — this is a refactor, not new feature

---

*Last Updated: January 3, 2026*
