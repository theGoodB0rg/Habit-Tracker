# Sound Assets Sourcing & Licensing Template

You must verify each sound's license (prefer CC0 / public domain) before shipping.

## Target Event Sounds
| Event | File Name | Length Goal | Description | Source URL | License | Attribution Needed |
|-------|-----------|-------------|-------------|------------|---------|--------------------|
| Start | `timer_start.ogg` | ≤ 0.40s | Soft single chime (pleasant, neutral) | TBD | CC0 | No |
| Midpoint | `timer_mid.ogg` | ≤ 0.40s | Two light ascending notes | TBD | CC0 | No |
| Progress (optional) | `timer_progress_soft.ogg` | ≤ 0.25s | Very subtle tick / tap | TBD | CC0 | No |
| Final | `timer_final.ogg` | 0.5–0.8s | Richer 2–3 note cadence | TBD | CC0 | No |
| Overtime (future) | `timer_overtime.ogg` | ≤ 0.35s | Gentle ping (repeatable) | TBD | CC0 | No |

## Curation Guidelines
- Peak level ≤ -1 dBFS; short fade-in/out (≥5 ms) to avoid clicks.
- Avoid low frequency rumble (<180 Hz) & harsh peaks (>8 kHz boosted).
- Keep mono 22.05 kHz or 44.1 kHz (OGG q4–5).
- Distinct timbre per semantic group (start vs final) but coherent palette.

## Recommended CC0 / Public Domain Search Sources
(Manually open and verify license on each page before download.)
- https://freesound.org (Filter: License=Creative Commons 0, Tags: ui, chime, bell, click, soft)
- https://pixabay.com/sound-effects/ (Filter: Sound Effects, short chime / notification)
- https://mixkit.co/free-sound-effects/ (Check license; most are Mixkit Free License—evaluation needed if acceptable.)
- https://opengameart.org (Search: ui chime cc0)

## Example Candidate Queries (Freesound)
- "soft ui chime cc0"
- "notification ping cc0"
- "short bell ui cc0"

## Verification Checklist (per file)
1. Confirm page explicitly states CC0 / Public Domain.
2. Download file; inspect in editor (waveform, remove silence, normalize).
3. Loudness pass: peak normalize to -1 dB, then optional LUFS alignment.
4. Export to OGG: `ffmpeg -i input.wav -c:a libvorbis -qscale:a 4 output.ogg`
5. Update table above with Source URL & License.
6. Remove any original filename identifying the author if license demands anonymity (optional).

## Suggested Audio Post Chain (Audacity or CLI)
1. High-pass filter at 180 Hz.
2. Gentle compressor (optional) if dynamic spikes.
3. Normalize to -1 dBFS.
4. Fade out last 30–60 ms if abrupt tail.

## Optional ffmpeg Batch (Run manually after placing WAVs in `raw_src/`)
```
for %f in (raw_src\*.wav) do ffmpeg -y -i "%f" -c:a libvorbis -qscale:a 4 raw_out\%~nf.ogg
```
(Use PowerShell adaptation if needed.)

## Sound Pack Mapping (Current Code)
- Default pack expects: `timer_start.ogg`, `timer_mid.ogg`, `timer_progress_soft.ogg`, `timer_final.ogg` in `app/src/main/res/raw/`.
- Minimal pack uses start + final only.
- Silent / System packs do not require internal audio files.

## Adding / Replacing Files
1. Delete placeholder `.txt` stubs in `res/raw/`.
2. Add processed `.ogg` files with EXACT required filenames.
3. Rebuild; verify no warnings about missing resources.
4. Test play in app (Start / Mid / Final events) on real device at various volumes.

## Attribution (If Non-CC0)
If a file is not CC0 but still permissive:
- Confirm redistribution rights.
- Add required attribution line below:
```
Attribution:
- timer_final.ogg by <AuthorName> (<URL>) licensed under CC-BY 4.0
```
- If CC-BY, optionally embed short credits section inside app Settings > About (future task).

## Future Enhancements
- Add alternate sound packs (focus, minimal) via dynamic resource lists.
- Implement runtime amplitude scaling based on user preference.
- Optional: adaptive sound suppression during media playback detection.

## Status Tracking
| File | Added | Optimized | Verified on Device | Notes |
|------|-------|-----------|--------------------|-------|
| timer_start.ogg |  |  |  |  |
| timer_mid.ogg |  |  |  |  |
| timer_progress_soft.ogg |  |  |  |  |
| timer_final.ogg |  |  |  |  |

---
(Complete this document before releasing a build that markets audio cues.)
