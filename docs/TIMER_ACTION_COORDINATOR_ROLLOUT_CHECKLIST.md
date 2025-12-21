# Timer Action Coordinator Rollout Checklist

**Flag**: `TimerFeatureFlags.enableActionCoordinator`

- Debug builds: **enabled** for dogfooding.
- Release builds: **enabled** as of 2025-11-08 (coordinator default flipped to `true` after beta sign-off).

## 1. Preflight
- [x] All coordinator entry points (EnhancedHabitCard, TimerControlSheet, MiniSessionBar) route through `TimerActionHandler` when the flag is on. *(2025-11-07 review of timing components)*
- [x] Analytics wiring listens to `TimerActionTelemetryEffect` and no UI surface calls `AnalyticsViewModel` timer events directly. *(2025-11-07 debugger verification)*
- [x] `TimerService` idempotent start guard verified (double-start blocked, snackbar displayed). *(2025-11-05 device smoke test)*
- [x] Coordinator confirmation flows manually verified (below-minimum, discard, pomodoro early end). *(QA checklist 2025-11-06)*
- [x] Smoke tests: start -> pause -> resume -> complete; discard non-zero; log partial; switch active habit. *(2025-11-07 emulator run)*
- [x] Offline analytics export still captures sessions when coordinator is enabled. *(2025-11-07 export regression suite)*
- [x] `./gradlew assembleDebug` succeeds. *(latest run 2025-11-08)*
- [x] `./gradlew :app:testDebugUnitTest` (or nearest equivalent) succeeds locally. *(latest run 2025-11-08)*

## 2. Rollout Stages
1. **Internal (default on in debug builds)** (Status: completed 2025-11-07)
   - QA + devs validated flows with coordinator toggled on using the shared preference override.
   - Triage regressions in telemetry dashboards and crash reports (no blockers).
2. **Dogfood/Beta channel** (Status: completed 2025-11-10)
   - 2025-11-08: pushed `enable_action_coordinator=true` pref to 25 internal beta testers (QA roster).
   - 2025-11-09: Crashlytics, completion success %, and snackbar frequency stayed within SLA (Analytics Dashboard run #418).
   - 2025-11-10: go/no-go review with product + QA; green-light to start staged rollout.
3. **Staged Production** (Status: completed 2025-11-16)
   - 2025-11-11: shipped build 1.18.0 with coordinator overrideable via shared pref.
   - 2025-11-12: OTA preference push to 10% of production; monitored 48h (no regressions).
   - 2025-11-14: widened to 50%; timer completion success +1.5%, crash-free sessions 99.6%.
   - 2025-11-16: rolled out to 100% of users; metrics remained green through 24h post-launch.
4. **Launch** (Status: completed 2025-11-16)
   - Default flipped to `true` in `TimerFeatureFlags.Defaults` (commit 95eda65).
   - Pref override retained for rollback; legacy timer direct calls slated for removal in UIX-13 hardening sprint.

### Rollout Cadence Summary
| Stage | Window | Actions | Owners |
| ----- | ------ | ------- | ------ |
| Internal validation | 2025-11-05 - 2025-11-07 | Manual QA flows, doc updates | Timing squad |
| Beta toggle | 2025-11-08 - 2025-11-10 | Completed pref push to 25 testers; dashboards green; go/no-go approved | QA + Analytics |
| Staged production | 2025-11-11 - 2025-11-16 | Completed 10 -> 50 -> 100% rollout; metrics within SLA | Release engineering |
| Full launch | 2025-11-16 onward | Default enabled; pref override retained for rollback | Release engineering |

### Post-rollout Metrics Snapshot (2025-11-16 17:00 UTC)
- Crash-free sessions: **99.6%** (no coordinator-linked regressions).
- Timer completion success rate: **+1.5%** uplift versus pre-rollout baseline.
- Snackbar frequency steady at **0.42 per active user** (within UIX guardrail).
- No critical bugs or support tickets related to coordinator rollout.

## 3. Toggling Instructions
`TimerFeatureFlags` values persist in `shared_prefs/timer_feature_flags.xml`.

### Enable coordinator on a connected device (debug build)
```bash
adb shell "run-as com.habittracker.debug sh -c 'mkdir -p shared_prefs && cat <<\"EOF\" > shared_prefs/timer_feature_flags.xml
<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?>
<map>
    <boolean name=\"enable_alert_scheduling\" value=\"true\" />
    <boolean name=\"enable_action_coordinator\" value=\"true\" />
</map>
EOF'"
```

### Disable coordinator again
```bash
adb shell "run-as com.habittracker.debug sh -c 'mkdir -p shared_prefs && cat <<\"EOF\" > shared_prefs/timer_feature_flags.xml
<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?>
<map>
    <boolean name=\"enable_alert_scheduling\" value=\"true\" />
    <boolean name=\"enable_action_coordinator\" value=\"false\" />
</map>
EOF'"
```

### Verify current state
```bash
adb shell "run-as com.habittracker.debug cat shared_prefs/timer_feature_flags.xml"
```

For production package IDs, replace `com.habittracker.debug` with `com.habittracker`.

## 4. Clean-up Tasks After Launch
- [ ] Remove legacy timer controller entry points bypassing the coordinator.
- [ ] Collapse feature flag override helpers not needed post-launch.
- [ ] Update analytics dashboards to treat coordinator events as the source of truth.
- [ ] Archive this checklist with final launch date.
