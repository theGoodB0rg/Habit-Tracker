# Smart Timing Modular Roadmap

Purpose: Execute the smart timing enhancements in small, self-contained phases that fit comfortably in a token window. Each phase caps scope, files, and line changes, with clear success criteria and validation gates.

Guardrails per micro‑phase:
- Max files edited: 3 (prefer 1–2)
- Max net changes: ~200 lines total
- No public API breakage; non-breaking migrations only
- Run build + targeted tests after each phase
- Add/adjust tests in the same phase when behavior changes

References:
- Core models/entities/DAOs exist under `core-architecture/...`
- UI models & screens under `app/src/main/java/com/habittracker/ui/...`
- Timing repository entry point: `app/.../data/repository/timing/TimingRepository.kt`

Token-safe session pattern:
1) Read relevant files (<= 3 files, full file or key sections)
2) Patch minimal changes
3) Build task: Build Habit Tracker App
4) Run/adjust unit tests (only related files)
5) Commit checkpoint notes (in this doc)

---

## Phase 0 — Safety & Inventory (no code changes)
Scope:
- Confirm build passes and DB version alignment.
- Identify exact files to touch in upcoming phases.

Deliverables:
- Short checklist outcome appended to this roadmap.

Validation:
- Run build task; no changes expected.

---

## Phase 1 — JSON serialization for timing fields
Problem:
- Converters currently use `.toString()` and ignore JSON for breaks/slots/triggers/metadata.

Scope:
- Introduce a single JSON adapter (Moshi or kotlinx.serialization) in a tiny utility.
- Implement real serialization/parsing in:
  - `core-architecture/.../converter/TimingEntityMappings.kt`
  - `app/.../data/database/converter/UiModelMappings.kt`

Deliverables:
- `JsonAdapters.kt` with tested adapters.
- Updated mapping functions parsing/writing JSON.
- Unit tests for roundtrips (breaks, time slots, triggers, metadata).

Constraints:
- Edit <= 3 files; tests in 1 new file.

Validation:
- Build passes.
- Tests: mapping roundtrip green.

---

## Phase 2 — Hydrate HabitUiModel with timing data
Problem:
- UI model sets timing/timer/metrics to null; no hydration path.

Scope:
- Implement `app/.../ui/models/timing/HabitUiModelExtensions.kt` to compose a HabitUiModel with timing, active session, suggestions, analytics via repository calls.
- Minimal integration point: add a small helper in the list/detail ViewModel to use the extension when fetching habits (no large refactors).

Deliverables:
- `toUiModelWithTiming(...)` usage wired.
- Smoke test: one ViewModel unit test to assert fields populated from repository fakes.

Constraints:
- <= 2 edits + 1 new test.

Validation:
- Build + unit test green.

---

## Phase 3 — Persist SmartTimingPreferences via DataStore
Problem:
- Preferences are in-memory only.

Scope:
- Add `TimingPreferencesDataStore` (DataStore) with schema for preferences.
- Wire `TimingFeatureViewModel` to load/save via repository interface.

Deliverables:
- DataStore class, DI provider, small repository wrapper.
- Unit test verifying read/write defaults and updates.

Constraints:
- 3 files + 1 test.

Validation:
- Build + unit test green.

---

## Phase 4 — Persist UserBehaviorMetrics
Problem:
- Behavior metrics not persisted.

Scope:
- Add a lightweight persistence (DataStore) for `UserBehaviorMetrics`.
- Replace placeholders in `TimingFeatureViewModel` with repository calls.

Deliverables:
- Metrics DataStore, DI, repository methods.
- Unit test for increment/update flows.

Constraints:
- 3 files + 1 test.

Validation:
- Build + unit test green.

---

## Phase 5 — Timer runtime (service + ticker)
Problem:
- No real runtime engine; repo toggles DB flags only.

Scope:
- Implement a small `TimerService` that emits ticks via a `MutableSharedFlow` or `LocalBroadcast`.
- APIs: start/pause/resume/complete; update DB via `TimingRepository`.

Deliverables:
- Foreground service with CountDownTimer-like logic (supports pause/resume).
- Tiny `TimerController` to bridge UI and service.
- Unit test for pure timing math (tick, pause, resume, complete).

Constraints:
- Keep service minimal; 2–3 files + 1 test.

Validation:
- Build; unit test of timing math green.

---

## Phase 6 — UI hook-up for timer controls
Problem:
- UI shows buttons but handlers are placeholders.

Scope:
- Wire `EnhancedHabitCard` and `TimingUIComponents` to call `TimerController` (start/pause/resume).
- Show live remaining time via Flow (no complex visuals yet).

Deliverables:
- Minimal state holder (ViewModel or remember) subscribing to ticks.
- Basic instrumentation test optional; at least a unit test for ViewModel state transitions.

Constraints:
- 2 edits + 1 new ViewModel/test.

Validation:
- Build + tests green.

---

## Phase 7 — Minimal SmartSuggestion generation
Problem:
- `generateSmartSuggestions` exists but limited.

Scope:
- Extract simple pattern analysis into a small engine class; keep in-memory, local only.
- Cover: best time by frequency; optional duration suggestion by successful sessions.

Deliverables:
- `PatternSuggestionEngine` with 2 methods and tests.
- Wire into `TimingRepository.generateSmartSuggestions`.

Constraints:
- 2 files + 1 test.

Validation:
- Build + tests green.

---

## Phase 8 — Analytics enrichment (non-breaking)
Problem:
- Analytics computes few metrics; JSON fields unused.

Scope:
- Compute consistency, average session duration, best/worst time; populate JSON time slots minimally (top N hour bands) and parse back.

Deliverables:
- Updated `updateHabitAnalytics` with helper functions.
- Parsing in mappings enabled for time slots.
- Unit tests validating computed values on sample data.

Constraints:
- 2 edits + 1 test.

Validation:
- Build + tests green.

---

## Phase 9 — File hygiene & DI cleanup
Problem:
- Empty repo files and a stray `.broken` file.

Scope:
- Delete `HabitTiming.kt.broken`.
- Either remove empty `SmartTimingRepository*.kt` or implement a thin interface redirecting to `TimingRepository`.

Deliverables:
- Removed/implemented files; DI graph unchanged.
- Simple compile-only validation.

Constraints:
- <= 3 file changes.

Validation:
- Build green.

---

## Phase 10 — Progressive Discovery completeness
Problem:
- Feature intros and level-ups not persisted; no UX telemetry.

Scope:
- Persist feature-first-seen/used fields in metrics store.
- Honor `showLevelUpPrompts` and record level-up events.

Deliverables:
- Extended metrics model + DataStore schema migration (non-breaking defaults).
- Unit tests for transitions and prompt gating.

Constraints:
- 2–3 files + 1 test.

Validation:
- Build + tests green.

---

## Phase 11 — Optional: Widget minimal timing surface
Scope:
- Show “Next suggested time” or timer-enabled indicator in widget.

Deliverables:
- Minimal widget data binding; no interactions yet.
- Manual screenshot verification.

Constraints:
- 1–2 edits.

Validation:
- Build + manual check.

---

## Phase 12 — Polish: error handling, performance, a11y, docs
Scope:
- Add error surfaces for DataStore/DB failures.
- Ensure no main-thread DB access; remember/Flow collection best practices.
- Update README and in-app help.

Deliverables:
- Try/catch + Result types where appropriate.
- README section “Smart Timing”.

Validation:
- Build; quick lint; smoke test app.

---

## Per‑phase Checklist (template)
- Inputs/outputs contract noted
- Edits ≤ 3 files, ≤ ~200 LOC
- Build passes
- New/updated tests pass
- No API breaks
- Notes appended to this roadmap

## Phase Queue (to run next)
1) Phase 1 — JSON serialization for timing fields
2) Phase 2 — Hydrate HabitUiModel with timing data
3) Phase 3 — Persist SmartTimingPreferences via DataStore

Appendix — Quick contracts
- Mapping contract: domain/UI <-> entity serialization is lossless for breaks, time slots, triggers, metadata.
- Timer contract: start/pause/resume/complete updates DB session row; emits ticks at 1s granularity; survives process recreation minimally (running flag honored).
- Suggestion contract: generation is idempotent per day; confidence ∈ [0,1]; records acceptance via DAO.
