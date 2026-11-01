# Project context

| Item | Value |
|------|--------|
| Name | ffmpeg-editor |
| Type | Terminal NLE (MVP → v0.2 roadmap) |
| Language | Java 17 |
| UI | Lanterna 3.x |
| Media | FFmpeg / ffprobe |
| Build | Gradle 8.5 (wrapper) · Maven (`pom.xml`) optional |
| Package | `com.videoeditor` |
| License | MIT |

## Layers

1. **domain** — `Project`, `Clip`, `Timeline`, `Track`, `Effect`, `ExportSettings`
2. **application** — `ImportMediaCommand`, `ExportProjectCommand`
3. **infrastructure** — FFmpeg adapters + JSON `ProjectRepository`
4. **presentation.tui** — decomposed: `VideoEditorApp`, `EditorRenderer`, `EditorInputHandler`, `EditorActions`, `EditorUiState`

## Docs map

| Doc | Audience |
|-----|----------|
| [../README.md](../README.md) | New contributor / GitHub |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Implementer |
| [SPRINTS.md](SPRINTS.md) | Planning |
| [RUNBOOK.md](RUNBOOK.md) | Operator / debug |
| [CHANGELOG.md](CHANGELOG.md) | Releases |

## Notes

- Prefer `./gradlew` over a system Gradle install.
- Lombok is required for `Project` and `Clip`.
- Sprint plan starts at Sprint 1 after stabilization (Sprint 0).
