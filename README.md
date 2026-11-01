# FFmpeg Editor

**Terminal video editor** — cut, arrange, filter and export media through FFmpeg, without leaving your shell.

Built with Java 17, [Lanterna](https://github.com/mabe02/lanterna) TUI and a clean hexagonal layout.

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" />
  <img alt="FFmpeg" src="https://img.shields.io/badge/FFmpeg-required-007808?style=flat-square&logo=ffmpeg&logoColor=white" />
  <img alt="Build" src="https://img.shields.io/badge/Build-Gradle%208.5-02303A?style=flat-square&logo=gradle&logoColor=white" />
  <img alt="License" src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" />
  <img alt="Status" src="https://img.shields.io/badge/Status-MVP-orange?style=flat-square" />
</p>

---

## Why

Most NLE tools are heavy GUIs. This project keeps the editing loop in the terminal: import → timeline → effects → export, while FFmpeg does the real work.

| Area | What you get |
|------|----------------|
| Media | Import files, inspect duration / resolution / codecs |
| Timeline | Video & audio tracks, ASCII timeline, playhead, split / delete |
| Effects | Fade in/out, watermark text, mute, speed change |
| Export | MP4 / MKV / WebM · Fast / Balanced / High Quality · progress + cancel |
| Projects | Save / load versioned JSON |

---

## Quick start

**Requirements:** Java 17+, FFmpeg + ffprobe on `PATH`, UTF-8 terminal (≥ 80×24).

```bash
git clone https://github.com/ursusandwolf/ffmpeg-editor.git
cd ffmpeg-editor

# Gradle (recommended)
./gradlew run                              # Linux / macOS
gradlew.bat run                            # Windows
./gradlew run --args="clip.mp4 other.mkv"  # import on start

# or Maven
mvn -q compile exec:java
```

Verify tooling:

```bash
java -version
ffmpeg -version
ffprobe -version
```

---

## Controls

| Key | Action |
|-----|--------|
| `Ctrl+O` | Import media |
| `Ctrl+S` | Save project |
| `Ctrl+E` | Export |
| `Ctrl+R` | Refresh metadata |
| `←` `→` | Switch panel |
| `↑` `↓` | Move selection |
| `Enter` | Add media to timeline |
| `Delete` | Remove selection |
| `S` | Split clip at playhead |
| `+` | Add clip |
| `Space` | Reset playback |
| `Q` | Quit |

**Panels:** Media Library → Timeline → Inspector

**Typical flow:** `Ctrl+O` → select clip → `Enter` → arrange on timeline → `Ctrl+E`.

---

## Architecture

```
com.videoeditor
├── domain                 # Project, Clip, Timeline, Track, Effect, ExportSettings
├── application/commands   # ImportMediaCommand, ExportProjectCommand
├── infrastructure
│   ├── ffmpeg             # FfprobeClient, FfmpegCommandBuilder, FfmpegJobRunner
│   └── persistence        # ProjectRepository (JSON)
└── presentation/tui       # VideoEditorApp (Lanterna)
```

Domain stays free of FFmpeg/UI details; adapters talk to the process and the screen.

<details>
<summary>Example FFmpeg commands the builder emits</summary>

```bash
# basic export
ffmpeg -y -preset medium -i input.mp4 -c:v libx264 -c:a aac -b:v 5000000 output.mp4

# fade in
ffmpeg -y -i input.mp4 -vf "fade=t=in:st=0:d=1" -c:a copy output.mp4

# watermark
ffmpeg -y -i input.mp4 -vf "drawtext=text='My Video':fontcolor=white:fontsize=24:x=10:y=10" -c:a copy output.mp4

# trim
ffmpeg -y -i input.mp4 -ss 10 -to 30 -c copy output.mp4
```

</details>

---

## Build & test

```bash
./gradlew build          # compile + tests + fat jar
./gradlew test
./gradlew test --tests DomainModelTest

# Maven alternative
mvn test
mvn package
```

Logs: `logs/video-editor.log` (see `src/main/resources/logback.xml`).

Project JSON schema version `1.0`:

```json
{
  "schemaVersion": "1.0",
  "id": "uuid",
  "name": "Project Name",
  "mediaAssets": [],
  "timeline": {},
  "settings": {}
}
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `ffmpeg` / `ffprobe` not found | Install FFmpeg and add it to `PATH` |
| Broken TUI layout | Use a UTF-8 terminal, resize to at least 80×24 |
| Import fails | Check the file path and that the container is supported by FFmpeg |

---

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, TUI split, key flows |
| [docs/SPRINTS.md](docs/SPRINTS.md) | Sprint 0–5 plan with acceptance criteria |
| [docs/RUNBOOK.md](docs/RUNBOOK.md) | Start, troubleshoot, escalate |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | Release notes |

## Roadmap

Tracked in [docs/SPRINTS.md](docs/SPRINTS.md). Near-term Must items:

- [ ] In-TUI import path prompt + project load/schema check
- [ ] Split / trim / playhead editing
- [ ] Effects wired into export `filter_complex`
- [ ] Executor shutdown + resize-safe TUI
- [ ] CI + `v0.2.0` release

Later: transitions, snapping, low-res preview, headless export-only mode.

---

## License

MIT © [ursusandwolf](https://github.com/ursusandwolf)

**Credits:** [Lanterna](https://github.com/mabe02/lanterna) · [FFmpeg](https://ffmpeg.org/) · workflow ideas inspired by classic NLE timelines
