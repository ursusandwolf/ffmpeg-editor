# Video Editor MVP

A minimal viable video editor with TUI (Text User Interface) built with Java, Lanterna, and FFmpeg.

## Features

### Media Management
- Import video files
- View metadata (duration, resolution, codec, etc.)
- Media library panel

### Timeline Editing
- Add clips to timeline
- Video and audio tracks
- ASCII visualization of timeline
- Playhead navigation
- Delete clips

### Effects (via FFmpeg filters)
- Fade in/out
- Watermark text overlay
- Mute audio
- Speed change

### Export
- Multiple formats: MP4, MKV, WebM
- Quality presets: Fast, Balanced, High Quality
- Configurable resolution and bitrate
- Progress tracking
- Cancel support

### Project Management
- Save/load projects as JSON
- Versioned schema

## Requirements

- Java 21+
- FFmpeg and ffprobe installed and in PATH
- Terminal with UTF-8 support

## Installation

```bash
# Clone the repository
git clone <repository-url>
cd video-editor

# Build with Gradle
./gradlew build

# Run the application
./gradlew run
```

## Usage

### Keyboard Controls

| Key | Action |
|-----|--------|
| `Ctrl+O` | Import media file |
| `Ctrl+S` | Save project |
| `Ctrl+E` | Export project |
| `Ctrl+R` | Refresh metadata |
| `←/→` | Navigate between panels |
| `↑/↓` | Navigate within panel |
| `Enter` | Add selected media to timeline |
| `Delete` | Remove selected item |
| `S` | Split clip at playhead |
| `+` | Add clip to timeline |
| `Space` | Reset playback |
| `Q` | Quit application |

### Panels

1. **Media Library** (left) - Shows imported media files
2. **Timeline** (center) - Visual timeline with tracks and clips
3. **Inspector** (right) - Shows details of selected item or export settings

### Workflow

1. Press `Ctrl+O` to import a video file (provide path via command line argument)
2. Select the media in the library and press `Enter` to add to timeline
3. Arrange clips on the timeline
4. Press `Ctrl+E` to export the final video

## Architecture

```
com.videoeditor
├── domain              # Core business entities
│   ├── Project
│   ├── MediaAsset
│   ├── Timeline
│   ├── Track
│   ├── Clip
│   ├── Effect
│   └── ExportSettings
├── application         # Use cases / commands
│   └── commands
│       ├── ImportMediaCommand
│       └── ExportProjectCommand
├── infrastructure      # External adapters
│   ├── ffmpeg
│   │   ├── FfprobeClient
│   │   ├── FfmpegCommandBuilder
│   │   └── FfmpegJobRunner
│   └── persistence
│       └── ProjectRepository
└── presentation        # UI layer
    └── tui
        └── VideoEditorApp
```

## Example FFmpeg Commands Generated

### Basic Export
```bash
ffmpeg -y -preset medium -i input.mp4 -c:v libx264 -c:a aac -b:v 5000000 output.mp4
```

### With Fade Effect
```bash
ffmpeg -y -i input.mp4 -vf "fade=t=in:st=0:d=1" -c:a copy output.mp4
```

### With Watermark
```bash
ffmpeg -y -i input.mp4 -vf "drawtext=text='My Video':fontcolor=white:fontsize=24:x=10:y=10" -c:a copy output.mp4
```

### Trim Video
```bash
ffmpeg -y -i input.mp4 -ss 10 -to 30 -c copy output.mp4
```

## Configuration

### Logging

Logs are written to `logs/video-editor.log`. Configure in `src/main/resources/logback.xml`.

### Project Format

Projects are saved as JSON with schema version 1.0:

```json
{
  "schemaVersion": "1.0",
  "id": "uuid",
  "name": "Project Name",
  "mediaAssets": [...],
  "timeline": {...},
  "settings": {...}
}
```

## Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests DomainModelTest
```

## Troubleshooting

### FFmpeg not found
Ensure FFmpeg is installed and available in your PATH:
```bash
ffmpeg -version
ffprobe -version
```

### Terminal display issues
Ensure your terminal supports UTF-8 and has adequate size (minimum 80x24).

### Import fails
Check that the video file is not corrupted and is in a supported format.

## Roadmap (v0.2)

- [ ] File dialog for importing media
- [ ] Drag-and-drop clip reordering
- [ ] Undo/Redo functionality
- [ ] Audio volume control
- [ ] Transition effects between clips
- [ ] Multi-track editing improvements
- [ ] Snapping for clip alignment
- [ ] Keyboard shortcut customization
- [ ] Preview rendering (low-res)
- [ ] Batch import
- [ ] Recent projects list
- [ ] Command-line interface mode

## License

MIT License

## Credits

- [Lanterna](https://github.com/mabe02/lanterna) - TUI library
- [FFmpeg](https://ffmpeg.org/) - Media processing engine
- Inspired by Sony Vegas Pro workflow concepts
