# Architecture — ffmpeg-editor

## 1. Context and goal

Terminal NLE that builds FFmpeg graphs from a domain model and runs them locally.

**Users:** developers / power users editing short clips in a shell.  
**Success:** import → timeline edit → export produces a playable file via system FFmpeg.

## 2. Scope

**In scope:** local TUI, JSON projects, FFmpeg/ffprobe subprocesses.  
**Out of scope:** networked collaboration, GUI, cloud assets, hardware encoding UI.

## 3. Layered design

```
presentation.tui          Lanterna UI (render / input / actions)
        │
application.commands      ImportMediaCommand, ExportProjectCommand
        │
domain                    Project, Timeline, Track, Clip, Effect, ExportSettings
        │
infrastructure            FfprobeClient, FfmpegCommandBuilder, FfmpegJobRunner,
                          ProjectRepository
```

| Layer | Responsibility | May depend on |
|-------|----------------|---------------|
| `domain` | Entities and invariants | JDK only |
| `application` | Use cases | domain + infrastructure ports (concrete for MVP) |
| `infrastructure` | OS processes, JSON I/O | domain |
| `presentation` | Terminal UX | application + domain |

## 4. TUI decomposition

| Type | Role |
|------|------|
| `VideoEditorApp` | Bootstrap, screen lifecycle, poll loop |
| `EditorRenderer` | Frame drawing |
| `EditorInputHandler` | Key → navigation / actions |
| `EditorActions` | Import, save, export, timeline mutations |
| `EditorUiState` | Selection, playhead, status, export flag |
| `ScreenCanvas` | Lanterna draw helpers |
| `TuiText` / `TimelineMetrics` / `PanelId` | Pure helpers / enums |

## 5. Key flows

### Import

1. Path from CLI args or (Sprint 1) TUI prompt  
2. `ImportMediaCommand` → `FfprobeClient.probeAsync`  
3. `MediaAsset` + metadata map added to `Project`

### Export

1. `ExportProjectCommand` validates timeline has clips  
2. `FfmpegCommandBuilder.buildExportCommand`  
3. `FfmpegJobRunner.runAsync` with progress callback  
4. Escape → `destroyForcibly`

### Persist

`ProjectRepository` serializes `Project` with Jackson + `JavaTimeModule` to JSON.

## 6. Data

**Project JSON (schema 1.0):** `id`, `name`, `mediaAssets[]`, `timeline`, `settings`, `schemaVersion` via `Project.getSchemaVersion()`.

**Clip timing:** source `[startTime, endTime)` on asset; `timelinePosition` on track.

## 7. Risks and constraints

| Risk | Mitigation |
|------|------------|
| Defensive copies on `Track.getClips()` | Mutate via `addClip` / `insertClip` / `removeClip` |
| FFmpeg filter graphs grow complex | Keep MVP concat; expand in Sprint 3 |
| Executor threads after quit | Sprint 4: shutdown hooks on clients |
| Regex ffprobe parsing | Replaced with Jackson `JsonNode` |

## 8. Build

- Primary: Gradle 8.5 wrapper, Java 17 toolchain, main class `com.videoeditor.presentation.tui.VideoEditorApp`
- Optional: Maven `pom.xml` (keep deps in sync when changing libraries)
