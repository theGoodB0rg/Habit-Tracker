# Crash Investigation Report: Prebundled Habit Click Crash

**Date:** December 20, 2025  
**Status:** UNRESOLVED - Needs further investigation

---

## Problem Summary

The app crashes with `ArrayIndexOutOfBoundsException` when clicking on **prebundled habits** (habits that are auto-created with completion history). User-created habits that have never been completed work fine.

### Key Observation
- **Works:** Clicking user-created habits with `lastCompletedDate == null` → Shows snackbar message
- **Crashes:** Clicking prebundled habits with `lastCompletedDate != null` → Triggers navigation → CRASH

---

## Stack Trace (Consistent across all crashes)

```
java.lang.ArrayIndexOutOfBoundsException: length=0; index=-5
    at androidx.compose.runtime.SlotTableKt.key(SlotTable.kt:3522)
    at androidx.compose.runtime.SlotTableKt.access$key(SlotTable.kt:1)
    at androidx.compose.runtime.SlotReader.groupKey(SlotTable.kt:957)
    at androidx.compose.runtime.ComposerImpl.end(Composer.kt:2357)
    at androidx.compose.runtime.ComposerImpl.endGroup(Composer.kt:1607)
    at androidx.compose.runtime.ComposerImpl.endRoot(Composer.kt:1483)
    at androidx.compose.runtime.ComposerImpl.doCompose(Composer.kt:3317)
    ...
    at androidx.compose.ui.layout.LayoutNodeSubcompositionsState.subcomposeInto(SubcomposeLayout.kt:500)
    at androidx.compose.ui.layout.LayoutNodeSubcompositionsState.subcompose(SubcomposeLayout.kt:472)
    ...
    at androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScopeImpl.measure-0kLqBqw(LazyLayoutMeasureScope.kt:125)
    at androidx.compose.foundation.lazy.LazyListMeasuredItemProvider.getAndMeasure(LazyListMeasuredItemProvider.kt:48)
    at androidx.compose.foundation.lazy.LazyListMeasureKt.measureLazyList-5IMabDg(LazyListMeasure.kt:195)
    at androidx.compose.foundation.lazy.LazyListKt$rememberLazyListMeasurePolicy$1$1.invoke-0kLqBqw(LazyList.kt:313)
```

---

## Prebundled Habits (Created in HabitRepository.kt)

These habits are created by `insertEnhancedDummyData()` with pre-set completion dates:
1. **Morning Meditation** - Has completion history
2. **Workout Session** - Has completion history  
3. **Learning Code** - Has completion history
4. **Family Time** - Has completion history
5. **Budget Review** - Has completion history

---

## Trigger Flow

```
MainScreen (LazyColumn)
  └── EnhancedHabitCard (onClick)
       └── if (habit.lastCompletedDate != null)
            └── onNavigateToHabitDetail(habit.id)  ← NAVIGATION TRIGGERS CRASH
                 └── HabitTrackerNavigation.kt composable
                      └── HabitDetailScreen
```

---

## Fixes Attempted (All Failed)

### 1. Debounce Click Handler
- **File:** Created `app/src/main/java/com/habittracker/ui/modifiers/DebouncedClickable.kt`
- **Change:** Applied `debouncedClickable` to EnhancedHabitCard
- **Result:** ❌ Still crashes

### 2. Navigation Safety (launchSingleTop)
- **File:** `HabitTrackerNavigation.kt`
- **Change:** Added `launchSingleTop = true` to all navigation calls
- **Result:** ❌ Still crashes

### 3. Removed State Collection from Navigation Composable
- **File:** `HabitTrackerNavigation.kt` lines 150-196
- **Change:** Removed `collectAsState` inside HabitDetail composable block to prevent race conditions
- **Before:** Had if/else checking `targetHabit.lastCompletedDate` inside composable
- **After:** Directly renders HabitDetailScreen
- **Result:** ❌ Still crashes

### 4. Upgraded Compose BOM
- **File:** `app/build.gradle`
- **Change:** `2023.10.01` → `2024.02.00`
- **Result:** ❌ Still crashes

### 5. Added Key to Grid LazyColumn Items
- **File:** `MainScreen.kt` line 479
- **Change:** Added `key = { habitPair -> habitPair.map { it.id }.joinToString("-") }` to chunked items
- **Result:** ❌ Still crashes

---

## Current State of Modified Files

### app/build.gradle
- Compose BOM: `2024.02.00` (upgraded from `2023.10.01`)
- kotlinCompilerExtensionVersion: `1.5.8` (unchanged)

### HabitTrackerNavigation.kt
- Simplified HabitDetail composable (no state collection inside)
- All navigations have `launchSingleTop = true`

### MainScreen.kt
- Grid items have proper keys

### DebouncedClickable.kt (NEW FILE)
- Located at: `app/src/main/java/com/habittracker/ui/modifiers/`
- Provides `debouncedClickable` modifier

### EnhancedHabitCard.kt
- Uses `debouncedClickable` on main Card

---

## Hypotheses for Next Investigation

### 1. AnimatedVisibility/AnimatedContent in EnhancedHabitCard
The card has multiple `AnimatedVisibility` blocks (lines ~428, ~546, ~1043) that may corrupt SlotTable during navigation transition.

**Action:** Try removing/disabling all AnimatedVisibility in EnhancedHabitCard temporarily.

### 2. hiltViewModel() Inside Composables
`HabitDetailScreen.kt` and `TimingSessionAnalyticsCard` call `hiltViewModel()` which creates ViewModels during composition.

**Files to check:**
- `HabitDetailScreen.kt` line 256: `val profileVm: AlertProfilesViewModel = hiltViewModel()`
- `HabitDetailScreen.kt` line 340: `val analyticsVm: HabitTimingAnalyticsViewModel = hiltViewModel()`

**Action:** Move ViewModel creation outside of conditionally-rendered composables.

### 3. Navigation During LazyList Measurement
The crash happens during `LazyListMeasure` which suggests navigation is triggering while the LazyColumn is still measuring items.

**Action:** Wrap navigation call in `LaunchedEffect` or post to next frame:
```kotlin
onClick = {
    if (habit.lastCompletedDate != null) {
        // Delay navigation to next frame
        snackbarScope.launch {
            kotlinx.coroutines.delay(1)
            onNavigateToHabitDetail(habit.id)
        }
    }
}
```

### 4. SubcomposeLayout Conflict
The crash originates from `SubcomposeLayout` which is used by both LazyColumn and potentially AnimatedContent.

**Action:** Check if there are nested SubcomposeLayouts (Scaffold inside LazyColumn items, etc.)

### 5. Compose Version Incompatibility
The `index=-5` error is a known Compose bug. Current setup:
- BOM: 2024.02.00
- Kotlin Compiler Extension: 1.5.8

**Action:** Try BOM `2024.06.00` or newer with matching Kotlin version.

---

## Commands for Quick Testing

```powershell
# Build and install
.\gradlew.bat :app:installDebug

# Clear data and restart
adb shell pm clear com.habittracker; adb shell am start -n com.habittracker/.MainActivity

# Get crash logs
adb logcat -d | Select-String "FATAL EXCEPTION" -Context 0,25 | Select-Object -Last 30

# Clear logcat
adb logcat -c
```

---

## Files to Focus On

1. **`MainScreen.kt`** - Contains LazyColumn with habit cards
2. **`EnhancedHabitCard.kt`** - Complex card with AnimatedVisibility
3. **`HabitDetailScreen.kt`** - Target screen with hiltViewModel calls
4. **`HabitTrackerNavigation.kt`** - Navigation setup
5. **`HabitRepository.kt`** - `insertEnhancedDummyData()` creates prebundled habits

---

## Test Device
- Emulator: `emulator-5554`
- Device: SM-S9160
- Android: 9
- App Package: `com.habittracker`
