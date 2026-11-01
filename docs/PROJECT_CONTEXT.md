# Project context

| Item | Value |
|------|--------|
| Name | ffmpeg-editor |
| Type | Terminal NLE (MVP) |
| Language | Java 17 |
| UI | Lanterna 3.x |
| Media | FFmpeg / ffprobe |
| Build | Gradle 8.5 (wrapper) · Maven (`pom.xml`) optional |
| Package | `com.videoeditor` |

## Layers

1. **domain** — entities (`Project`, `Clip`, `Timeline`, …)
2. **application** — commands (import / export)
3. **infrastructure** — FFmpeg adapters + JSON repository
4. **presentation** — `VideoEditorApp` TUI

## Notes

- Prefer `./gradlew` over a system Gradle install.
- Lombok is required for `Project` and `Clip`.
- Empty stub docs (`UML.md`, `DOCUMENTATION.md`) were removed.
