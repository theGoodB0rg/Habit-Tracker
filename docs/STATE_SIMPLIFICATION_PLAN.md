# Timer State Simplification Plan

> **Living Document** — Update this file as phases are completed. Each phase must be validated with `./gradlew assembleDebug` before marking complete and committing.

**Created:** January 3, 2026  
**Status:** 🟡 In Progress  
**Goal:** Consolidate fragmented timer state management into a single source of truth, eliminating logic flow bugs and race conditions.

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

## Phase 1: State Consolidation

**Status:** 🔴 Not Started  
**Estimated:** 1-2 days  
**Validation:** `./gradlew assembleDebug` must pass

### Tasks

#### 1.1 Enhance CoordinatorState
**File:** [TimerActionCoordinator.kt](../app/src/main/java/com/habittracker/timerux/TimerActionCoordinator.kt)

- [ ] Add `targetMs: Long` to track total duration
- [ ] Add `pausedHabitId: Long?` for auto-paused timer tracking
- [ ] Add `pausedRemainingMs: Long` for auto-paused timer display
- [ ] Add `lastError: String?` for error state display
- [ ] Rename `waitingForService` → `isLoading` for clarity

#### 1.2 Consolidate TimerBus Event Handling
**File:** [TimerActionCoordinator.kt](../app/src/main/java/com/habittracker/timerux/TimerActionCoordinator.kt)

- [ ] Handle `TimerEvent.AutoPaused` → update `pausedHabitId`, `pausedRemainingMs`
- [ ] Handle `TimerEvent.Started` → update `targetMs` from event
- [ ] Clear `pausedHabitId` when user dismisses switch sheet or resumes

#### 1.3 Remove Redundant ViewModel Usage in MainScreen
**File:** [MainScreen.kt](../app/src/main/java/com/habittracker/ui/screens/MainScreen.kt)

- [ ] Remove `tickerViewModel: TimerTickerViewModel = hiltViewModel()` 
- [ ] Remove `activeTimerViewModel: ActiveTimerViewModel = hiltViewModel()`
- [ ] Remove `remainingByHabit`, `pausedByHabit` collections
- [ ] Replace with single `coordinatorState by timerActionHandler.state.collectAsStateWithLifecycle()`
- [ ] Simplify `LaunchedEffect` blocks — remove direct `TimerBus.events.collect`

#### 1.4 Update EnhancedHabitCard to Use Coordinator State Only
**File:** [EnhancedHabitCard.kt](../app/src/main/java/com/habittracker/ui/components/EnhancedHabitCard.kt)

- [ ] Remove `tickerViewModel` parameter and usage
- [ ] Remove `activeTimerVm` parameter and usage  
- [ ] Remove `partialSessionVm` parameter and usage
- [ ] Pass `coordinatorState` from parent instead of collecting in each card
- [ ] Derive `isActive`, `isPaused`, `remainingMs` from coordinator state

### Phase 1 Validation
```powershell
# Run from project root
./gradlew clean assembleDebug

# Expected: BUILD SUCCESSFUL
# If failed: Fix errors before proceeding
```

### Phase 1 Commit
```
git add -A
git commit -m "Phase 1: Consolidate timer state into TimerActionCoordinator

- Add targetMs, pausedHabitId, pausedRemainingMs to CoordinatorState
- Remove TimerTickerViewModel usage from MainScreen
- Remove ActiveTimerViewModel usage from MainScreen  
- EnhancedHabitCard now receives state from parent
- Simplified LaunchedEffect blocks in MainScreen

Validated: ./gradlew assembleDebug PASSED"
```

---

## Phase 2: UI Simplification

**Status:** 🔴 Not Started  
**Estimated:** 1-2 days  
**Depends on:** Phase 1 Complete

### Tasks

#### 2.1 Extract Timer Section from EnhancedHabitCard
**New File:** `app/src/main/java/com/habittracker/ui/components/timer/HabitTimerSection.kt`

- [ ] Create `HabitTimerSection` composable
- [ ] Props: `habitId`, `timerState`, `remainingMs`, `targetMs`, `onStart`, `onPause`, `onResume`, `onComplete`
- [ ] Collapsed state: Show chip "Start Xm"
- [ ] Expanded state: Show time + controls + progress bar

#### 2.2 Simplify EnhancedHabitCard
**File:** [EnhancedHabitCard.kt](../app/src/main/java/com/habittracker/ui/components/EnhancedHabitCard.kt)

- [ ] Replace inline timer UI with `HabitTimerSection`
- [ ] Remove interactor fallback logic (coordinator is always available now)
- [ ] Consolidate 4 confirmation dialogs into single `TimerConfirmationDialog` component
- [ ] Target: Reduce file from ~1293 lines to ~600 lines

#### 2.3 Update TimerSwitcherSheet to Use Coordinator State
**File:** [TimerSwitcherSheet.kt](../app/src/main/java/com/habittracker/ui/components/timer/TimerSwitcherSheet.kt)

- [ ] Remove internal ViewModel lookups
- [ ] Props: `activeHabitId`, `activeHabitName`, `activeRemainingMs`, `pausedHabitId`, `pausedHabitName`, `pausedRemainingMs`
- [ ] All data passed from MainScreen (from coordinator state)

#### 2.4 Update MiniSessionBar
**File:** [MiniSessionBar.kt](../app/src/main/java/com/habittracker/ui/components/timer/MiniSessionBar.kt)

- [ ] Remove internal state collection
- [ ] Props: `habitName`, `remainingMs`, `paused`, `onPause`, `onResume`, `onComplete`
- [ ] Parent provides all data from coordinator state

### Phase 2 Validation
```powershell
./gradlew clean assembleDebug
```

### Phase 2 Commit
```
git add -A
git commit -m "Phase 2: Simplify timer UI components

- Extract HabitTimerSection composable
- Reduce EnhancedHabitCard complexity (~600 lines from ~1300)
- TimerSwitcherSheet receives all data from parent
- MiniSessionBar is now stateless
- Unified confirmation dialog component

Validated: ./gradlew assembleDebug PASSED"
```

---

## Phase 3: Polish & Edge Cases

**Status:** 🔴 Not Started  
**Estimated:** 1 day  
**Depends on:** Phase 2 Complete

### Tasks

#### 3.1 Loading States
- [ ] Add skeleton loading when `uiState.isLoading` on MainScreen
- [ ] Show spinner on action button when `coordinatorState.isLoading`
- [ ] Disable all timer controls during loading

#### 3.2 Error Handling
- [ ] Display `coordinatorState.lastError` as dismissible banner
- [ ] Auto-clear error after 5 seconds
- [ ] Add retry action to error banner

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
