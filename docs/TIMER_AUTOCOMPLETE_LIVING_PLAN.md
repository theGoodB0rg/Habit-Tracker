# Timer Auto-Complete Living Plan

A single source of truth for the timer-to-habit completion work. Keep this file updated as phases are validated, builds/tests are run, and commits are made. Designed so any assistant can pick up the effort in a new conversation.

## Current next actions
- Finalize Phase 2 spec: confirm the embedded rules are approved and mark Phase 2 ready for implementation.
- Phase 3 validation: add tests for timer-completion path and run build (e.g., ./gradlew test assembleDebug).
- Phase 4 implementation/validation: period-key uniqueness now added (schema + migration + repo/widget idempotency). Next: add tests and run build.

## Authoritative directives (do not dilute)
- This plan is fixed from the user’s supplied phases and guardrails. Do not “reinterpret” or simplify; keep these intact.
- Every phase below must be validated (tests/builds) before committing, using the specified commit labels.
- Keep this file living: when a phase completes, record validation details and the commit hash here.

### User-supplied phase outline
- **Phase 1 — Baseline discovery**: Locate timer completion flow; confirm auto vs manual rules; identify recurrence fields; no code changes; write findings; no commit.
- **Phase 2 — Define correct behavior and guardrails**: Specify rules; define UX; prevent duplicates in active period; deliver spec; no commit.
- **Phase 3 — Implementation: timer completion -> habit completion**: Wire timer-finish to completion use-case with flags and idempotency; tests; commit “Wire timer completion to habit completion”.
- **Phase 4 — Duplicate prevention across periods**: Implement current-period key and duplicate blocking across app/widget/repo/DAO; tests; commit “Enforce single completion per period”.
- **Phase 5 — UI/UX adjustments**: Disable duplicate completes, show status/snackbar, immediate state update; tests where feasible; commit “Update completion UI states”.
- **Phase 6 — Regression and integration checks**: Full tests and smoke; persistence validation; commit only if fixes; label “Integration fixes for timer completion”.
- **Phase 7 — Acceptance & rollout notes**: Summarize behavior, flags, testing, rollback, limitations; doc commit if needed “Docs: timer auto-complete behavior”.

## Workflow rules
- Keep this doc living: update statuses, validation notes, and links to commits as work progresses.
- Each phase must have: what was changed, how it was validated (tests/builds), and whether APK builds succeeded.
- Do not commit unvalidated changes; record test/build commands and results here before committing.
- Use consistent commit labels per phase (see below). One commit per phase unless hotfixes are required.
- When resuming in a new session, read this file first and continue from the last incomplete phase.

## Phase checklist and guidance

### Phase 1 — Baseline discovery
- Goal: Map timer completion flow, completion storage, recurrence handling, and gating flags.
- Deliverable: Short findings note (no code). Record current gaps.
- Validation: None (analysis only). No commit.
- Status: Not started (fill with findings when done).

### Phase 2 — Define correct behavior and guardrails
- Goal: Finalize rules for auto-complete, early stop, recurrence key (daily/weekly/monthly), idempotency, UX responses, and failure handling. Align flags: askToCompleteWithoutTimer, requireTimerToComplete, minDurationSec, autoCompleteOnTarget, timerEnabled.
- Deliverable: Reviewable spec/checklist in this doc. 
- Validation: Peer/maintainer review; no code change. No commit.
- Status: In progress. Spec draft was provided by user; it is captured below and must remain authoritative.

**Phase 2 specification (authoritative rules)**
- **Completion trigger rules**
	- Auto-complete when a timer reaches natural end (remaining ≤ 0) and the session isn’t cancelled/stopped. Persist habit completion immediately from the service/coordinator path (no UI dependency).
	- If `requireTimerToComplete` is true: only timer-finish can complete; manual “complete without timer” is blocked unless user explicitly confirms an override.
	- Early stop:
		- If elapsed < `minDurationSec`: ask confirmation to complete anyway or log partial; default is do not complete.
		- If elapsed ≥ `minDurationSec` but < target: allow completion (no duplicate), optionally show “early finish” snackbar.
	- Partial logging: only when explicitly chosen; partials never mark completion.

- **Recurrence / period key**
	- Define “current period key” per habit frequency:
		- Daily: LocalDate
		- Weekly: ISO week (year + week number)
		- Monthly: year + month
		- (If a custom cadence exists, map accordingly; otherwise default to daily.)
	- Completion writes and duplicate checks use this key. If already completed for the period, subsequent completes are no-ops (and UI should reflect “Completed this period”).
	- Period rollover: availability resets when the period key changes; no manual reset needed.

- **Data & idempotency**
	- Persist completion via repository using the period key; keep DB unique index for (habitId, periodKey) or enforce in DAO logic if schema can’t change.
	- Idempotent handler: timer-finish completion should be safe to call multiple times; it should simply no-op if already completed for the active period.

- **UX behavior**
	- When auto-complete fires: show unobtrusive confirmation (snackbar/toast); update button states to “Completed this period”.
	- When user taps complete during the same period: show status instead of toggling; no duplicate records.
	- If completion is blocked (`requireTimerToComplete` and timer not run): show actionable message “Start the timer to complete this habit.”

- **Flags / settings**
	- Honor `askToCompleteWithoutTimer`: prompt before completing without an active timer.
	- Honor `requireTimerToComplete`: disallow manual complete unless a timer session finished or user confirmed override.
	- Honor `minDurationSec`, `autoCompleteOnTarget`, and `timerEnabled` as described above.

- **Failure behavior**
	- If persistence fails, show a snackbar error and do not flip UI state; retry only on explicit user action.
	- Timer completion should not be lost if UI isn’t active (service/coordinator must perform the persistence).

### Phase 3 — Implementation: timer completion -> habit completion
- Goal: Wire TimerService/TimerActionCoordinator completion to habit completion use-case respecting flags; ensure idempotency and no-op on duplicates.
- Deliverable: Code changes + tests for timer completion path.
- Validation: Unit/logic tests around timer completion; APK build (gradle assembleDebug). 
- Commit label: "Wire timer completion to habit completion".
- Status: In progress. Timer completion now persists via TimerEvent.Completed only (no double writes/UI). Tests/build not yet run.

### Phase 4 — Duplicate prevention across periods
- Goal: Implement “current period key” (daily/weekly/monthly) and enforce single completion per active period across app/widget/repo/DAO paths; auto-reset on period rollover.
- Deliverable: Data/DAO/repo logic (or schema) enforcing uniqueness; tests across daily/weekly/monthly.
- Validation: Unit tests for duplicates; APK build.
- Commit label: "Enforce single completion per period".
- Status: Implementation in progress (schema + migration + repo/widget idempotency added; tests/build pending).

**Phase 4 implementation so far**
- Added `periodKey` to `habit_completions` with unique index `(habitId, periodKey)` and migration 7→8 to backfill, dedupe, and enforce uniqueness.
- Shared `PeriodKeyCalculator` (daily yyyy-MM-dd, weekly isoYear-Www, monthly yyyy-MM).
- Repository paths (app core + widget) compute periodKey and check idempotency before writes; deletes use periodKey.
- Export mapping updated to include periodKey.

**Remaining validation for Phase 4**
- Add tests for daily/weekly/monthly duplicate prevention and mixed triggers (timer + manual, app + widget).
- Run build/tests (e.g., ./gradlew test assembleDebug).

### Phase 5 — UI/UX adjustments
- Goal: Update habit cards/detail to show completed-this-period state and disable duplicate completes; add snackbar/toast on auto-complete; button state updates immediately.
- Deliverable: UI/state updates + tests where feasible.
- Validation: UI/state tests if available; APK build.
- Commit label: "Update completion UI states".
- Status: Not started.

### Phase 6 — Regression and integration checks
- Goal: Full test sweep; smoke timer flows (start, pause, stop-early, complete, overtime); verify persistence and period logic survive process kill.
- Deliverable: Test report notes in this doc.
- Validation: Full test suite + targeted manual smoke; APK build.
- Commit label: "Integration fixes for timer completion" (only if fixes added).
- Status: Not started.

### Phase 7 — Acceptance & rollout notes
- Goal: Summarize behavior changes, flags, testing performed, rollback steps, known limitations.
- Deliverable: Final notes in this doc; optional code doc if needed.
- Validation: None (documentation). 
- Commit label: "Docs: timer auto-complete behavior" (only if repo docs change).
- Status: Not started.

## Validation log template (fill per phase)
- Date:
- Phase:
- Changes:
- Tests/commands run: (include gradle assembleDebug / test tasks, and results)
- Result: pass/fail, issues found
- Links: (commit hashes, PRs)

## Commit guidance
- One commit per phase after validation. Use the phase-specific labels above.
- If a phase needs follow-up fixes, append "(fix)" to the label or include a separate concise fix commit.
- Ensure commits only include scoped changes for that phase; avoid mixing phases.

## How to continue in a new session
1) Read this file to see the last completed/in-progress phase.
2) If a phase is "in progress", finish its deliverable and validation steps.
3) Update the Validation log and Phase status, then commit with the proper label when validation passes.
4) Move to the next phase and repeat.
