# Crash Post-Mortem: Compose Runtime & Layout Issues
**Date:** 2026-01-10
**Status:** Resolved

## 1. Runtime Crash: `ArrayIndexOutOfBoundsException`

### Incident
The app crashed consistently when opening `HabitDetailScreen` with the following trace:
`java.lang.ArrayIndexOutOfBoundsException: length=73; index=-1`
at `androidx.compose.runtime.Stack.pop(Stack.kt:32)`

### Root Cause: Conditional Early Return in Composable
The crash was caused by an **early return** inside the `AnalyticsOverviewCard` composable function.

**Problematic Pattern:**
In Jetpack Compose, the compiler tracks the structure of the UI tree using a "slot table". When a Composable function returns early based on a condition, it disrupts the expected slot table structure if the compiler isn't able to track the group exit correctly relative to the group entry. This "group imbalance" causes the runtime to try to "pop" a group from the stack that doesn't exist or is in the wrong state, leading to `index=-1`.

### Code Fix

**❌ BAD (Caused Crash):**
```kotlin
@Composable
private fun AnalyticsOverviewCard(metrics: CompletionMetrics?) {
    Card {
        Column(...) {
            Text("Analytics")
            
            // 💀 DANGER: Early return inside a Scope scope (Column)
            if (metrics == null) {
                Text("No data yet")
                return@Column // <--- CRASH SOURCE
            }

            // ... other UI elements
        }
    }
}
```

**✅ GOOD (Fixed):**
```kotlin
@Composable
private fun AnalyticsOverviewCard(metrics: CompletionMetrics?) {
    Card {
        Column(...) {
            Text("Analytics")
            
            // Safe: Standard Branching
            if (metrics == null) {
                Text("No data yet")
            } else {
                // ... other UI elements
            }
        }
    }
}
```

### Prevention
*   **Avoid `return` in Composables**: Never use `return` (or `return@Scope`) to exit a Composable early if you have already emitted UI nodes (like `Text` or `Card`) before it.
*   **Use `if/else`**: Always wrap the alternative content in an `else` block.
*   **Null Checks First**: If possible, handle null checks *before* entering the UI structure (e.g., `if (data == null) return` at the very top of the function, before any `Column` or `Box`).

---

## 2. Layout Crash: Nested Scrollables

### Incident
Earlier crashes occurred in `SimpleMainScreen` and potentially `EditHabitScreen` due to improper nesting of lazy components.

**Error:** `java.lang.IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints.`

### Root Cause
This happens when you place a `LazyColumn` or `LazyVerticalGrid` inside a parent that is already vertically scrollable (like a `Column` with `.verticalScroll()`).
*   The parent gives "infinite" height to chunks of content.
*   The child (`LazyColumn`) tries to fill "all available space".
*   "All available" of "Infinite" = Crash.

### Code Fix (EditHabitScreen)

**❌ BAD (Would Crash):**
```kotlin
Column(modifier = Modifier.verticalScroll(scrollState)) { // Parent scrolls
    // ... input fields
    
    // 💀 CRASH: LazyGrid inside Scrollable Column
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.height(200.dp) // Even with height, this is risky/bad practice
    ) { 
        items(icons) { Icon(...) } 
    }
}
```

**✅ GOOD (Applied Solution):**
```kotlin
Column(modifier = Modifier.verticalScroll(scrollState)) { // Parent scrolls
    // ... input fields
    
    // Safe: Manual Layout (Column + Row + forEach)
    // Since the item count is small (~20 icons), we don't need lazy loading.
    val rows = icons.chunked(5)
    Column { 
        rows.forEach { rowIcons ->
            Row { 
                rowIcons.forEach { icon -> Icon(...) }
            }
        }
    }
}
```

### Prevention
*   **Never Nest Same-Direction Scrollables**: Do not put `LazyColumn` inside `Column(Modifier.verticalScroll)`.
*   **Use `LazyColumn` for the Whole Screen**: If a screen needs to scroll, the *entire* screen should usually be one `LazyColumn` with different `item { ... }` blocks for the static content.
*   **Manual Layout for Small Lists**: If a nested list is small (e.g., < 50 items), use a simple `Column`/`Row` loop instead of the heavy `Lazy` components.
