# Smart Timing — Full Integration Execution Plan (UI-Visible)

Purpose: Deliver a professional, seamless, and functional Smart Timing experience that users can actually use. This plan stitches together the roadmap phases into concrete UI surfaces, wiring, error handling, a11y, and validation so the features appear in the app and widget.

Status snapshot (from latest screenshot):
- Habit list shows title, description, streak chip, frequency chip, edit, and complete.
- Missing: timer controls, live countdown, active-session badge, suggestion chips, analytics enrichment, preferences entry, error surfaces, and widget timing.

Goals:
- Make Smart Timing features visible and usable on the Habit Card, Details, and Widget.
- Ensure correctness, responsiveness, a11y, and reliability (DataStore/DB error handling, no main-thread DB access).
- Keep changes incremental (≤3 files per PR where practical) and validated by builds/tests.

Scope summary (what users will see):
- Habit Card: Start/Pause/Resume/Complete controls, mm:ss countdown, “Active” badge, suggestion chips (e.g., Best time 7–8 PM, Start 10m), and small analytics hints where space allows.
- Habit Details: Richer analytics (consistency %, avg session duration, best time band) with context text.
- Settings/Preferences: Smart Timing toggles (enable, show level-ups), default durations, and suggestion behavior.
- Widget: Minimal “Next suggested time” or timer-enabled indicator.
- Error surfaces: Non-blocking banners/toasts for DataStore/DB failures; retry affordances where appropriate.

Success criteria (acceptance):
- A user can start a timer from a habit card, see a live countdown, pause/resume, and complete the session, with DB updates.
- Suggestions show when available; tapping a suggestion starts a session using the suggested duration.
- Analytics values are displayed in details and remain stable across app restarts (DataStore persisted preferences/metrics).
- Widget shows next suggested time and updates daily/minimally.
- No visible frame drops; no main-thread DB access; all Flows collected safely with lifecycle awareness.
- Build succeeds; targeted unit/instrumentation tests pass; quick lint shows no critical issues.

---

## Architecture & Data Flow

Modules involved:
- core-architecture: domain models/entities/DAOs; JSON mapping.
- app: UI, ViewModels, DI, repositories, DataStore wrappers, foreground service controller.
- widget-module: AppWidget provider/service for minimal timing surface.

Key flows:
- ViewModel -> TimingRepository -> DB/DAOs for sessions, analytics.
- ViewModel -> PreferencesDataStore + MetricsDataStore for persisted feature state.
- TimerController <-> TimerService for tick events via Flow/SharedFlow/LocalBroadcast; ViewModel subscribes and updates UI state.
- SuggestionEngine called by repository; results exposed as Flow<List<Suggestion>>.

Threading:
- Room/DAO queries off main; Flows with distinctUntilChanged + map on IO.
- DataStore read/write off main, with try/catch wrapping.

---

## UI Deliverables

Habit Card (EnhancedHabitCard):
- Timer buttons: Start, Pause/Resume, Complete (states swap contextually).
- Countdown text: mm:ss (or m:ss) with live ticks.
- Active badge: small accent chip when a session is running.
- Suggestion chips: “Best time 7–8 PM” and optional “Start 10m”. Hidden when unavailable.
- Error banner slot (ephemeral) for DataStore/DB operation failures.

Habit Details:
- Analytics block: consistency %, average session duration, best time band (hour range), with short helper text.

Widget:
- Text line: “Next suggested time: 7–8 PM” (or “None today”). No interactions in v1.

A11y:
- Content descriptions for controls and countdown; focus order; 44×44px touch targets; visible focus ring.
- Reduced motion respected; text scales with system settings; color contrast AA.

---

## Implementation Plan (PR-sized slices)

Slice 1 — Hydration + Minimal Timer on Card:
- Wire toUiModelWithTiming(...) in Habit list ViewModel so cards get timing/session/suggestion fields.
- Add TimerButtonGroup and Countdown in EnhancedHabitCard.
- Add TimerController bridging UI to TimerService; render ticks in card.
Files (typical):
- app/.../ui/models/timing/HabitUiModelExtensions.kt (new/updated)
- app/.../ui/habits/EnhancedHabitCard.kt (update) and a small TimerUIComponents.kt (new)
- app/.../timer/TimerController.kt (new), app/.../timer/TimerService.kt (new minimal)
Tests:
- ViewModel state transitions (start/pause/resume/complete) with fakes.
- Pure timing math test for tick/pause/resume.

Slice 2 — Suggestions Surface:
- PatternSuggestionEngine (if not present) or expand existing to return best-hour band + suggested duration.
- Show chips on card; CTA starts timer with suggested duration.
Files:
- core-architecture/.../suggestion/PatternSuggestionEngine.kt (new/update)
- app/.../data/repository/timing/TimingRepository.kt (wire-in)
- app/.../ui/habits/EnhancedHabitCard.kt (update to render chips)
Tests:
- Engine unit tests and repository integration test with sample data.

Slice 3 — Analytics Enrichment UI:
- Ensure updateHabitAnalytics computes consistency, avg session duration, best/worst time; parse JSON time slots.
- Show summarized metrics on Habit Details.
Files:
- core-architecture/.../analytics/AnalyticsUpdater.kt (update)
- core-architecture/.../converter/TimingEntityMappings.kt (JSON parsing enabled)
- app/.../ui/habitdetail/HabitDetailScreen.kt (render block)
Tests:
- Analytics computation on sample histories.

Slice 4 — Preferences & Metrics Persistence:
- TimingPreferencesDataStore (enable feature, default durations, showLevelUpPrompts).
- UserBehaviorMetricsDataStore (first-seen/used; simple counters).
- Surface a small Settings screen entry or overflow menu item linking to Smart Timing settings.
Files:
- app/.../data/datastore/TimingPreferencesDataStore.kt (new)
- app/.../data/datastore/UserBehaviorMetricsDataStore.kt (new)
- app/.../ui/settings/SmartTimingSettingsScreen.kt (new) and DI wiring
Tests:
- Read/write defaults; update flows; settings ViewModel simple tests.

Slice 5 — Widget Minimal Timing Surface:
- Bind “Next suggested time” from repository to widget text.
Files:
- widget-module/HabitsWidgetProvider.kt (update)
- widget-module/HabitsWidgetService.kt (update)
Tests:
- Manual screenshot + simple provider test if present.

Slice 6 — Error Handling, A11y, Performance Polish:
- Wrap DataStore/DB ops in try/catch; surface Result types to UI.
- Add error banner/toast on card and settings.
- Ensure no main-thread DB access; audit Flows and remember usage.
- Add content descriptions, focus order, and enlarged touch targets.
Files:
- app/... ViewModels and UI components (targeted updates)
- README “Smart Timing” section (add)
Tests:
- Error path tests; accessibility axe-like lint if applicable.

---

## Contracts (concise)

Timer contract:
- start(habitId, durationMs?) → emits ticks each 1s; persists running session row; survives process via flag; pause/resume/complete update session row.

Suggestion contract:
- generateSmartSuggestions(habitId, date) → List<Suggestion> with confidence ∈ [0,1]. Idempotent per day. Acceptance recorded via DAO when user taps.

Mapping contract:
- Domain/UI <-> entity serialization is lossless for breaks, time slots, triggers, metadata via JSON (Moshi or kotlinx.serialization).

Preferences/Metrics contract:
- DataStore-backed with non-breaking defaults and migrations; offline-only.

---

## Error Handling & UX

- DataStore failures: show inline banner “Couldn’t save Smart Timing settings. Retry.” with retry action.
- DB failures: show toast/banner “Timer update failed. State will retry.” and queue retry where safe.
- TimerService lifecycle: Foreground with proper notification; handle process death gracefully by restoring running state.

---

## Accessibility & Design

- Minimum 44×44px touch targets for controls; semantic labels: “Start timer”, “Pause timer”, “Resume timer”, “Complete session”.
- Countdown announced via polite live region when state changes; reduce updates to avoid screen reader spam.
- Respect prefers-reduced-motion; avoid jittery transitions; text scales.
- Contrast AA for chips and badges; focus-visible outlines.

---

## Performance & Reliability

- No main-thread DB/DataStore access; Room + coroutines with Dispatchers.IO.
- Flow throttling for ticks (coalesce to 1s) and UI recomposition minimization.
- Memoize suggestion/analytics where possible for the day.
- Keep service small; use SharedFlow(replay=1) for ticks.

---

## Testing & Validation

Unit tests:
- Timer math (tick/pause/resume/complete) and session persistence transitions.
- Suggestion engine outputs for synthetic histories.
- Analytics computations and JSON mapping roundtrips.
- DataStore read/write defaults and updates; error paths (simulated exceptions).

UI/Instrumentation (targeted):
- Habit card timer controls state transitions.
- Suggestion chip visible when engine returns a result and CTA starts timer.
- Details screen shows analytics.
- Widget shows next suggested time text.

Quality gates:
- Build: task “Build Habit Tracker App”.
- Lint: quick lint; no critical accessibility or threading violations.
- Smoke test: launch app, start/pause/resume/complete a timer, confirm DB updated.

---

## Developer Runbook

Build:
```powershell
# From workspace root
./gradlew.bat assembleDebug
```

Install & launch (optional):
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.habittracker -c android.intent.category.LAUNCHER 1
```

Capture screenshot:
```powershell
adb shell screencap -p /sdcard/ht_screencap.png
adb pull /sdcard/ht_screencap.png "C:\Users\HP\Desktop\Personal Websites\Offline_Habit_Tracker\screenshot_updated.png"
adb shell rm /sdcard/ht_screencap.png
```

---

## Risks & Mitigations

- Service lifecycle edge cases: use foreground service and restore state via repository on app resume; add notification channel.
- UI clutter: hide chips/controls when not applicable; compact layout; truncate long text gracefully.
- Data migrations: add default values and test read of legacy stores.

---

## Incremental PR Checklist (repeat per slice)

- [ ] Edits ≤ 3 files (~≤200 LOC net)
- [ ] Build green (assembleDebug)
- [ ] New/updated tests green
- [ ] No API breaks; DI intact
- [ ] A11y checks (labels, focus, targets)
- [ ] Notes appended to SMART_TIMING_MODULAR_ROADMAP.md

---

## Definition of Done (feature-level)

- Habit Card exposes working timer controls and live countdown
- Suggestions appear and start a session when tapped
- Details screen shows analytics computed from history
- Preferences screen persists settings; metrics tracked
- Widget shows “Next suggested time”
- Error surfaces present; no main-thread DB access
- README updated with “Smart Timing” section
