# UI / UX Enhancement Roadmap

> Scope: Progressive, modular upgrade of habit timer & overall interaction model (audio/TTS cues, visuals, personalization, accessibility, performance) while preserving existing Smart Timing gradual feature exposure.

## 0. Strategic Overview
- **Core Themes:** Clarity, Feedback, Delight, Personalization, Accessibility, Performance.
- **Principles:** Progressive disclosure (aligned with `TimingFeatureViewModel` levels), offline-first, low power, modular separation (service vs. alert engine vs. UI components), testability.
- **Primary Outcomes:** Higher completion rate, improved session adherence, reduced cognitive friction, richer insights, and scalable foundation for future multi-session or advanced contexts.

## 1. High-Level Goals
1. Per-habit timer enablement & customization.
2. Multi-channel alert & cue system (audio, TTS, haptics, visual micro-animations).
3. Rich timer visuals (radial progress, mini session bar, adaptive chips).
4. Enhanced detail & analytics: timing history, threshold editor, insight surfaces.
5. Profiles for alert behaviors (Quiet / Focus / Verbose / Custom).
6. Accessibility & responsiveness improvements (reduced motion, semantics, tablet layout, widget parity).
7. Performance optimization (recomposition throttling, scheduling precomputation).
8. Analytics & observability instrumentation.

## 2. Architecture Additions
| Layer | Addition | Purpose |
|-------|----------|---------|
| Data (DB) | HabitEntity fields: `timerEnabled`, `customDurationMinutes`, `alertProfileId` | Per-habit customization |
| Data (DB) | `TimerAlertProfileEntity` | Reusable alert profile definitions |
| DataStore | New prefs: `defaultAlertProfileId`, `enableGlobalAudioCues`, `reducedMotion`, optional future `ttsPreferredEngine` | Global behaviors |
| Service | Alert schedule generation + emission of `TimerAlertEvent` | Deterministic threshold dispatch |
| Engine Module | `AlertEngine` | Abstract audio/haptic/TTS routing |
| UI Module | `timer-ui` components (RadialTimer, MiniSessionBar, ThresholdEditor) | Composable reuse |
| Analytics | New event taxonomy | Behavioral insights |

## 3. Phasing Strategy
Phases are intentionally vertical slices; each ends in a testable, shippable increment. Dependencies minimized to allow rollback without cascading failures.

### Phase UIX-1: Foundations & Schema
**Scope:** DB migration, DataStore fields, seeding default alert profiles, per-habit timer toggle (hidden behind feature flag until validated).  
**Deliverables:**
- Migration script adds new columns (nullable-safe defaults).
- Repository updates + unit tests for migration integrity.
- Seed profiles: Quiet, Focus, Verbose (JSON thresholds definition).
- Edit Habit screen: (internal flag) toggle for timer enable; custom default duration.
**Acceptance Criteria:** Existing users unaffected; enabling toggle persists; profiles appear in logs.
**Metrics:** Migration success rate (no crashes), cold start time unchanged (< ±2%).
**Risks & Mitigation:** Schema mismatch → wrap migration in try/catch & fallback defaults.

### Phase UIX-2: Alert Scheduling in TimerService
**Scope:** Extend `TimerService` to precompute alert schedule & emit `TimerAlertEvent` via Flow (no real audio yet).  
**Deliverables:**
- `AlertType` enum & schedule builder.
- In-service pointer logic (advances on tick, ignore while paused, resumes safely).
- Test suite: schedule generation for 25m & custom durations; pause/resume boundaries.
**Acceptance Criteria:** Logs show correct event ordering; no duplicate alerts across pause/resume.
**Metrics:** CPU overhead negligible (<1% delta on tick loop sample).
**Risks:** Off-by-one timing drift → use monotonic elapsed differences.

### Phase UIX-3: AlertEngine (Audio/Haptic/TTS Infrastructure)
**Scope:** Non-UI implementation of delivery channels.  
**Deliverables:**
- `AlertEngine` facade (injectable) with strategy: chime (SoundPool/ExoPlayer), vibration patterns, optional TTS (prewarm + fallback).
- User preference gates for audio/haptics/TTS.
- Graceful downgrade when permission/audio focus denied.
**Acceptance Criteria:** Manual test: each alert logs chosen channel; fallback works (e.g., no TTS engine → chime).
**Metrics:** TTS init latency logged; no ANRs.
**Risks:** Audio focus contention → request transient focus; fallback to vibration.

### Phase UIX-4: Core Timer UI Components
**Scope:** Radial progress, adaptive time text, minimal recomposition, LiveRegion semantics.  
**Deliverables:**
- `RadialTimer` composable (Canvas + animated sweep + color gradient shift).
- `LiveRemainingTime` uses throttled Flow (only when visible).
- Unit snapshot tests for layout at different times.
**Acceptance Criteria:** Timer updates visibly every second, CPU profile shows controlled recompositions.
**Metrics:** Recompose count per active card <= 1/sec; jank frames < 3%.
**Risks:** Overdraw → keep vector draws minimal, reuse brushes.

### Phase UIX-5: Mini Session Bar & Global Persistence
**Scope:** Floating mini bar for active session with pause/resume/complete/extend.  
**Deliverables:**
- `MiniSessionBar` pinned to Scaffold bottom or top (configurable).
- State derived from existing ticker ViewModel.
- Accessibility actions (semantics for controls).
**Acceptance Criteria:** Appears only with active session, dismiss restores on navigation.
**Metrics:** Memory overhead minimal (<200 KB additional objects).
**Risks:** Gesture conflicts → use distinct elevation + shadow for clarity.

### Phase UIX-6: Profiles & Threshold Editor UI
**Scope:** User-facing profile selection + custom editor.  
**Deliverables:**
- Profiles screen (list + detail) with preview of active thresholds.
- Habit detail: assign profile override.
- Custom profile creation: threshold pickers & reorder drag.
**Acceptance Criteria:** Profile changes reflected in newly started sessions; persistent after restart.
**Metrics:** Profile CRUD operations success log; no orphaned profiles.
**Risks:** Complex drag UX → fallback to simple add/remove list if reordering unstable.

### Phase UIX-7: Rich Notifications & Extend Flow
**Scope:** Upgrade notification content + actions (Pause/Resume/Extend +5m/Complete).  
**Deliverables:**
- Custom notification layout (compat path for < API 24 via standard style).
- Heads-up option for final 10s (opt-in).
- Extend +5m modifies schedule & service counters atomically.
**Acceptance Criteria:** Actions perform correctly; extend recalculates remaining & alert schedule.
**Metrics:** Notification action latency <500ms.
**Risks:** Foreground service channel importance mismatch → dynamic channel importance check.

### Phase UIX-8: Habit Detail Timing Analytics
**Scope:** Visual insight surfaces (sparkline, baseline delta, explanation of suggestion).  
**Deliverables:**
- Sparkline (last N sessions durations vs target line).
- Average duration & adherence badge.
- “Why this suggestion?” explanation card (pull from existing suggestion engine context data).
**Acceptance Criteria:** All visuals fallback gracefully when insufficient data (<3 sessions).
**Metrics:** Crash-free rendering across orientation changes.
**Risks:** Over-fetching DB → pre-aggregate via repository.

### Phase UIX-9: Accessibility & Inclusive Refinements
**Scope:** Semantics, reduced motion, contrast, haptics preference alignment.  
**Deliverables:**
- ContentDescriptions for radial progress (% + remaining).
- Reduced motion preference disables pulse animations.
- High contrast token mapping + QA.
**Acceptance Criteria:** TalkBack announces: “Timer 8 minutes remaining (32 percent).”
**Metrics:** Accessibility lint passes; no added ANRs.
**Risks:** Over-verbose announcements → throttle LiveRegion updates (polite).

### Phase UIX-10: Performance & Battery Optimization
**Scope:** Profiling + optimization pass.  
**Deliverables:**
- Visibility-based subscription (only active Composables collect ticks).
- Benchmark test measuring battery/time (baseline vs new).
- Remove any redundant snapshot conversions.
**Acceptance Criteria:** Battery impact within baseline margin (<5% variance over 30m test). Faster cold start unaffected.
**Metrics:** Jank % vs baseline; CPU usage sampling.
**Risks:** Premature optimization complexity → document rationale per change.

### Phase UIX-11: Advanced Engagement & Overtime Nudges
**Scope:** Overtime detection + gentle nudge & analytics events.  
**Deliverables:**
- Overtime alert at +1m if user hasn’t completed.
- Nudge card in app offering “Log as extended” or “Complete now”.
**Acceptance Criteria:** Overtime not fired if session manually completed on time.
**Metrics:** Overtime resolution actions captured.
**Risks:** User annoyance → one nudge cap per session.

### Phase UIX-12: Widget & External Surface Parity
**Scope:** Home screen widget shows active countdown & controls.  
**Deliverables:**
- Existing widget-module adaptation (progress ring, pause/complete intents, color states).
- Consistent alert profile icon glyph.
**Acceptance Criteria:** Widget updates within 1–2s of state change (respecting OS constraints).
**Metrics:** Update rate compliance; no ANR in widget host.
**Risks:** Excessive updates → apply throttling when screen off.

### Phase UIX-13: Analytics & Export Enhancements
**Scope:** Finalize telemetry + export structure.  
**Deliverables:**
- New analytics events (timer_alert_fired, timer_profile_applied, etc.).
- Export includes per-habit timer settings + profile IDs + aggregated adherence metrics.
**Acceptance Criteria:** Export JSON validates against schema; PII excluded.
**Metrics:** Event delivery success rate.
**Risks:** Payload bloat → compress large arrays server-side (future scope).

### Phase UIX-14: Final QA, Polish & Launch Package
**Scope:** Consolidated regression testing, docs, feature flags cleanup.  
**Deliverables:**
- Test matrix results (devices, API levels, dark/light, reduced motion on/off).
- README / in-app help update for timer features.
- Kill temporary debug flags.
**Acceptance Criteria:** All blocking bugs resolved; performance & a11y gates pass.
**Metrics:** Zero new critical issues after internal beta run.
**Risks:** Scope creep → lock changes except P0 bug fixes.

## 4. Cross-Cutting Concerns
| Concern | Approach |
|---------|----------|
| Progressive Level Gating | Map feature visibility to engagement levels; guard editor & custom profiles. |
| Error Handling | Central ephemeral banner (already pattern exists) + structured logs. |
| Testing | Each phase adds unit + targeted instrumentation; avoid end-loaded QA. |
| Observability | Lightweight logger abstraction enabling later remote analytics plug-in. |
| Performance | Profile every 2–3 phases (Systrace / JankStats) to prevent regression accumulation. |

## 5. Data & Event Schemas (Illustrative)
`TimerAlertEvent`: `{ habitId: Long, sessionId: Long, alertType: String, remainingMs: Long, emittedAt: Long }`
`timer_profile_applied`: `{ habitId, profileId, mode }`
`timer_extended`: `{ habitId, addedMs, newTargetMs }`

## 6. Acceptance Gate (Per Phase)
1. Build green (CI).
2. Unit + added integration tests green.
3. No new lint / detekt violations introduced.
4. Accessibility scan (where relevant) passes.
5. Docs updated if user-facing change.

## 7. Rollback Strategy
- Each phase behind a feature flag (Gradual release toggles: `EnableAlertProfiles`, `EnableMiniSessionBar`, etc.).
- DB changes additive & backward-compatible (no destructive migrations until final consolidation).

## 8. Open Assumptions / Clarifications
| Assumption | Impact if Wrong |
|------------|-----------------|
| Single active session at a time | Multi-session would require session registry & per-habit service model. |
| “ts audio” = TTS requirement | If incorrect, we can trim TTS from roadmap (remove Phase elements). |
| Users value extend +5m over arbitrary add time | If not, add configurable extend increments. |

## 9. KPIs / Success Metrics
- +X% increase in timer session completions (baseline vs 30 days post-launch). *(Define X after baseline capture.)*
- Reduced average late completions by Y%. 
- Engagement uplift: increase in users reaching Engaged/PowerUser levels.
- App stability: no increase in crash rate (remain < current baseline).

## 10. Deferred / Future Ideas (Out of Current Scope)
- Multi-habit stacked session mode.
- Contextual ambient suggestions (location / time-of-day hybrid triggers).
- Wear OS companion & cross-device sync.
- ML-based adaptive duration prediction.

## 11. Execution Checklist Snapshot
(Will be expanded into issue tracker; kept here for high-level traceability.)
- [ ] UIX-1 Foundations
- [ ] UIX-2 Scheduling
- [ ] UIX-3 AlertEngine
- [ ] UIX-4 Timer UI
- [ ] UIX-5 Mini Session Bar
- [ ] UIX-6 Profiles UI
- [ ] UIX-7 Notifications
- [ ] UIX-8 Detail Analytics
- [ ] UIX-9 Accessibility
- [ ] UIX-10 Performance
- [ ] UIX-11 Overtime Nudges
- [ ] UIX-12 Widget Parity
- [ ] UIX-13 Analytics & Export
- [ ] UIX-14 Final Polish

---
**Ready for Implementation:** Begin with Phase UIX-1 after confirmation. Adjust assumptions if necessary before migrations are committed.
