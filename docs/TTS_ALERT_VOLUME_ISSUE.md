# Issue: Timer TTS alerts are too quiet

## Diagnosis
- Voice prompts (e.g., "Timer started", "Timer complete") are played via TextToSpeech in [app/src/main/java/com/habittracker/timing/alert/AlertEngine.kt](app/src/main/java/com/habittracker/timing/alert/AlertEngine.kt#L110-L147).
- The speak call uses default parameters (`TextToSpeech.speak` with null params); no stream, audio focus, or per-utterance volume are set. The app’s master volume slider only scales SoundPool chimes, not TTS.
- TTS volume therefore follows the device/emulator TTS or media stream level and can be very low. No in-app normalization exists.
- Gating logic confirms spokenText is provided when prefs enable TTS, but still uses defaults; see [app/src/main/java/com/habittracker/timing/TimerService.kt](app/src/main/java/com/habittracker/timing/TimerService.kt#L103-L137) and [app/src/main/java/com/habittracker/timing/TimerService.kt](app/src/main/java/com/habittracker/timing/TimerService.kt#L840-L876).

## Goal
Make TTS alerts audible and predictable by explicitly routing them to the media stream, applying app-configured volume, and ensuring focus/fallback handling.

## Plan (phased, each with its own commit)

### Phase 1 – Instrumentation and logging
- Add detailed logs: TTS init status, selected engine, language result, and each speak request (event type, volume applied, stream, focus result).
- Guard against null spokenText to avoid silent calls.
- Validation: run preview buttons and live timer start/final; verify logs show TTS ready and speak entries with expected volume/stream.
- Commit message: "Add TTS alert instrumentation"

### Phase 2 – Control TTS routing and volume
- Set explicit AudioAttributes for TTS (usage ASSISTANCE_ACCESSIBILITY or MEDIA, content type SPEECH) and route to STREAM_MUSIC.
- Apply per-utterance volume derived from soundMasterVolumePercent (and optional minimum floor, e.g., 0.2) so TTS follows app volume.
- Request transient audio focus before speaking; abandon after completion.
- Add graceful fallback: if TTS not ready, log and optionally play chime/haptic only.
- Validation: emulator/device tests with various system volumes and another media app playing; confirm consistent loudness and no suppression.
- Commit message: "Route timer TTS to media stream with volume control"

### Phase 3 – UX setting (optional but recommended)
- Add a dedicated "Voice volume" slider (or reuse master slider explicitly) in settings; persist in TimingPreferences and apply in AlertEngine.
- Update previews to reflect the slider.
- Validation: change slider and observe TTS loudness scaling accordingly; verify persistence across app restarts.
- Commit message: "Add voice volume preference for timer TTS"

### Phase 4 – Regression checks
- Run existing timer flows: start, pause/resume, complete; ensure chimes and haptics unchanged.
- Check that silent/minimal/system sound packs still behave (TTS respects enableTts; sound pack gating unaffected).
- Commit message: "Validate TTS alert delivery"

## Progress log
- 2025-12-21: Implemented Phase 1 & Phase 2 in code (AlertEngine). Added TTS init/speak logging, explicit audio attributes, media stream routing, per-utterance volume with floor, and transient audio focus handling. Pending validation runs and commits.

## Notes
- Keep this file updated as phases complete; append outcomes and links to commits for future reference.
