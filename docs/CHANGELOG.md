# Changelog

## [Unreleased]

### Fixed
- `splitClip` now inserts into the track (`insertClip`) instead of a defensive list copy
- MUTE uses `-af volume=0`; SPEED_CHANGE only applies with valid speed; watermark escaping hardened
- ffprobe metadata parsed via Jackson (removed brittle regex)
- Project save no longer NPEs on relative paths without a parent
- TUI status bar no longer clears the whole screen; Quit no longer NPEs on Escape
- Process helpers drain stdout and restore interrupt status

### Changed
- TUI decomposed into renderer / input / actions / state / canvas helpers
- CLI args import media on startup

### Docs
- Architecture, sprint plan, runbook

## [0.1.0] — 2026-11-01

### Repository
- Consolidated code onto `main`
- Removed tracked `.gradle/` cache and bundled `gradle-8.5/`
- Added Gradle Wrapper 8.5, MIT LICENSE, GitHub README

### App
- MVP TUI editor: import, timeline, effects, export
- Lanterna 3.x compatibility
- Lombok on `Project` / `Clip`
- Domain + FFmpeg command builder tests
