# Runbook — ffmpeg-editor

## Start

```bash
# requirements: Java 17+, ffmpeg, ffprobe on PATH
./gradlew run                         # Linux/macOS
gradlew.bat run                       # Windows

# optional: import files at startup
./gradlew run --args="clip1.mp4 clip2.mp4"
```

```bash
./gradlew test
./gradlew build   # produces fat jar under build/libs
```

## Symptom → check → fix

| Symptom | Check | Fix |
|---------|-------|-----|
| App warns FFmpeg not found | `ffmpeg -version` / `ffprobe -version` | Install FFmpeg; add to `PATH`; restart terminal |
| Import fails / duration zero | Probe JSON / file integrity | Re-encode or use a supported container; check logs in `logs/video-editor.log` |
| Export fails immediately | Status bar error + log command | Ensure timeline has clips; output path writable; codecs available in FFmpeg build |
| Export hangs | Process list shows `ffmpeg` | Press Escape to cancel; if stuck, kill process and restart app |
| TUI garbled / wrong layout | Terminal size & encoding | UTF-8 terminal ≥ 80×24; avoid broken legacy code pages |
| Save fails | Path / permissions | Ensure cwd writable; parent dirs for nested paths are created when present |
| Build fails on Lombok | Annotation processing | Use `./gradlew` (Lombok configured); IDE: enable annotation processing |
| Process won't exit after Quit | Non-daemon executors | Known gap until Sprint 4; kill JVM if hung |

## Logs

- File: `logs/video-editor.log` (Logback: `src/main/resources/logback.xml`)
- Useful lines: built FFmpeg argv, ffprobe failures, export exit codes

## Escalation data

Collect: OS, Java version, `ffmpeg -version`, project JSON (no private media paths if sensitive), last 50 log lines, exact key sequence.
