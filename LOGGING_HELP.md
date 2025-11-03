Stable ADB Logging in VS Code (Windows)
======================================

Use the provided scripts/tasks to avoid terminal instability:

- ADB: Capture logcat to file — starts a fresh capture to `logcat_full.txt` in the repo root (runs via cmd script).
- ADB: View filtered log (tail) — shows a live filtered view from the saved file.
- ADB: Live log (cmd, tee-like) — live log and save via a safe cmd → PowerShell tee pipeline.

If the integrated terminal crashes:
- Default terminal is set to Command Prompt. You can switch to PowerShell (NoProfile) in the dropdown.
- GPU acceleration and shell integration are disabled for stability.
- You can always open an external terminal and run `scripts\\adb_log_capture.cmd` directly.

Troubleshooting:
- Ensure at least one device is connected: `adb devices`
- If `adb` is not in PATH, install Android Platform Tools and add to PATH, or run via Android SDK `platform-tools` directory.
- Logs are cleared before capture (`adb logcat -c`).
