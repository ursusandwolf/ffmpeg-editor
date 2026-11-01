# Sprint plan — ffmpeg-editor

**Horizon:** 6 sprints × 1 week  
**Baseline:** MVP TUI (v0.1) after cleanup, review fixes and TUI decomposition  
**Success metric:** usable local NLE loop — import → edit → export — without leaving the terminal

## Assumptions

- One developer (or small pair), Java 17 + FFmpeg available
- No cloud storage / multi-user / GUI in this horizon
- Gradle Wrapper is the primary build path

## Non-goals (all sprints)

- Full DaVinci/Vegas feature parity
- Real-time GPU preview player
- Plugin marketplace
- Windows installer / signed releases

---

## Sprint 0 — Stabilization *(done / closing)*

| ID | Item | Priority | Acceptance |
|----|------|----------|------------|
| S0-1 | Repo hygiene (wrapper, LICENSE, README, single `main`) | Must | Clone + `./gradlew test` documented |
| S0-2 | Code review blockers (splitClip, MUTE `-af`, ffprobe Jackson, TUI wipe) | Must | Tests green; known bugs from review closed |
| S0-3 | Decompose `VideoEditorApp` | Must | Presentation split into renderer / input / actions / state |

**Exit:** v0.1 tagged mentally as “stable MVP core”

---

## Sprint 1 — Import & project I/O

**Goal:** operator can load media and persist work without CLI-only hacks.

| ID | Story | Priority | Acceptance (G/W/T) |
|----|-------|----------|--------------------|
| S1-1 | In-TUI path prompt for import | Must | Given focus in Media, When Ctrl+O and path entered, Then asset appears with metadata |
| S1-2 | Batch import from CLI args | Must | Given `gradlew run --args="a.mp4 b.mp4"`, When app starts, Then both assets are in library |
| S1-3 | Save/Load with schema check | Must | Given `project.json`, When load, Then schemaVersion `1.0` validated or clear error |
| S1-4 | Autosuggest recent projects | Should | Given prior saves in cwd, When open recent, Then list shows newest first |

**Tests:** import command unit tests; repository load/save round-trip; schema mismatch test  
**Risk:** blocking stdin in Lanterna — spike path prompt early

---

## Sprint 2 — Timeline editing

**Goal:** clips are editable on the timeline with correct domain mutations.

| ID | Story | Priority | Acceptance |
|----|-------|----------|------------|
| S2-1 | Split at playhead | Must | Given clip under playhead, When `S`, Then two clips cover original range without gap/overlap |
| S2-2 | Trim in/out from inspector | Must | Given selected clip, When trim values set, Then duration and export command update |
| S2-3 | Move playhead with ←/→ while timeline focused | Must | Playhead moves by fixed step; ruler updates |
| S2-4 | Delete + selection sync | Must | After delete, selection index stays in range |
| S2-5 | Undo last edit (single-level stack) | Should | Ctrl+Z restores previous timeline snapshot |

**Tests:** `ImportMediaCommand.splitClip` / `trimClip` cases; timeline metrics after split  
**Risk:** defensive copies on collections — use track mutators only

---

## Sprint 3 — Effects & export quality

**Goal:** effects applied in export; presets produce distinct, valid FFmpeg graphs.

| ID | Story | Priority | Acceptance |
|----|-------|----------|------------|
| S3-1 | Attach fade / watermark / mute / speed on selected clip | Must | Inspector shows effect list; export command includes filters |
| S3-2 | Export uses clip effects (not only `buildEffectCommand` helper) | Must | `buildExportCommand` merges per-clip filters into `filter_complex` |
| S3-3 | Resolution & bitrate honored | Must | `-s` / `-b:v` match `ExportSettings` |
| S3-4 | Cancel export mid-run | Must | Escape kills process; UI returns to Ready |
| S3-5 | Progress % estimate from `time=` vs total | Should | Status shows monotonic progress string |

**Tests:** command-builder snapshots for multi-clip + effects; job runner cancel test (mocked process if possible)  
**Risk:** `atempo` range 0.5–2.0 — chain filters for extreme speeds

---

## Sprint 4 — UX polish & reliability

**Goal:** daily use without terminal glitches.

| ID | Story | Priority | Acceptance |
|----|-------|----------|------------|
| S4-1 | Panel focus keys `1`/`2`/`3` | Must | Focus jumps to Media / Timeline / Inspector |
| S4-2 | Resize-safe layout (min 80×24) | Must | Below minimum: warning status, no crash |
| S4-3 | Shutdown: close screen + FFmpeg/ffprobe executors | Must | Process exits without hanging non-daemon threads |
| S4-4 | Structured logging of export command | Should | Log line contains argv list, not shell-joined ambiguous string |
| S4-5 | Headless smoke: `--export-only project.json` | Could | Exit 0 on success without TUI |

---

## Sprint 5 — Hardening & release

**Goal:** shippable v0.2.

| ID | Story | Priority | Acceptance |
|----|-------|----------|------------|
| S5-1 | CI: build + test on push (GitHub Actions) | Must | Badge green on `main` |
| S5-2 | Fat jar artifact + run instructions | Must | `java -jar` starts TUI |
| S5-3 | Integration test with real ffprobe fixture (small sample) | Should | Probe asserts duration > 0 |
| S5-4 | Changelog + tag `v0.2.0` | Must | Release notes list S1–S4 Must items |
| S5-5 | Remove dead helpers or wire them (e.g. `escapeArg` policy) | Should | No unused public API without docs |

---

## Backlog (post v0.2)

- Transitions between clips  
- Multi-track snap / ripple edit  
- Low-res preview render  
- Audio volume envelopes  
- Custom keybindings file  

## Capacity guide

| Sprint | Focus | Rough size |
|--------|--------|------------|
| 0 | Stabilization | — |
| 1 | I/O | M |
| 2 | Timeline | L |
| 3 | Effects/export | L |
| 4 | UX | M |
| 5 | Release | M |

**Definition of Done (every Must story):** code + test or manual AC checklist in PR; docs updated if user-facing.
