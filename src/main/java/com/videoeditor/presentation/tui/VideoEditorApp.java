package com.videoeditor.presentation.tui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.videoeditor.application.commands.ExportProjectCommand;
import com.videoeditor.application.commands.ImportMediaCommand;
import com.videoeditor.domain.*;
import com.videoeditor.infrastructure.ffmpeg.FfprobeClient;
import com.videoeditor.infrastructure.persistence.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main TUI application using Lanterna.
 */
public class VideoEditorApp {
    private static final Logger log = LoggerFactory.getLogger(VideoEditorApp.class);
    
    private Screen screen;
    private Project project;
    private ImportMediaCommand importCommand;
    private ExportProjectCommand exportCommand;
    private ProjectRepository projectRepository;
    
    private int selectedPanel = 0; // 0=media, 1=timeline, 2=inspector
    private int selectedMediaIndex = 0;
    private int selectedClipIndex = 0;
    private Duration playheadPosition = Duration.ZERO;
    
    private AtomicBoolean isExporting = new AtomicBoolean(false);
    private String statusMessage = "Ready";
    private String lastProgress = "";

    public static void main(String[] args) {
        VideoEditorApp app = new VideoEditorApp();
        try {
            app.run();
        } catch (Exception e) {
            log.error("Application error", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public void run() throws IOException {
        // Initialize components
        project = new Project();
        importCommand = new ImportMediaCommand();
        exportCommand = new ExportProjectCommand();
        projectRepository = new ProjectRepository();
        
        // Check FFmpeg availability
        if (!exportCommand.isFfmpegAvailable()) {
            System.err.println("Warning: FFmpeg not found. Export functionality will be limited.");
        } else {
            log.info("FFmpeg available: {}", exportCommand.getFfmpegVersion());
        }
        
        // Setup terminal
        Terminal terminal = new DefaultTerminalFactory()
            .setInitialTerminalSize(120, 40)
            .createTerminal();
        
        screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.setCursorPosition(null);
        
        log.info("Video Editor TUI started");
        
        // Main loop
        boolean running = true;
        while (running) {
            render();
            
            KeyStroke keyStroke = screen.pollInput();
            if (keyStroke == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            
            running = handleInput(keyStroke);
        }
        
        cleanup();
    }

    private void render() throws IOException {
        screen.clear();
        
        int height = screen.getTerminalSize().getRows();
        int width = screen.getTerminalSize().getColumns();
        
        // Header
        renderHeader(width);
        
        // Main panels (3 columns)
        int panelWidth = (width - 4) / 3;
        int contentHeight = height - 8;
        
        renderMediaPanel(2, 2, panelWidth, contentHeight);
        renderTimelinePanel(panelWidth + 4, 2, panelWidth * 2 - 2, contentHeight / 2);
        renderInspectorPanel(panelWidth + 4, contentHeight / 2 + 3, panelWidth * 2 - 2, contentHeight / 2);
        
        // Status bar
        renderStatusBar(width, height);
        
        screen.refresh();
    }

    private void renderHeader(int width) {
        String title = "═══ Video Editor MVP ═══";
        int padding = (width - title.length()) / 2;
        
        screen.putString(padding, 0, title, TextCharacter.DEFAULT);
        screen.putString(2, 0, "Project: " + project.getName(), TextCharacter.DEFAULT);
        
        // Help text
        String help = "Ctrl+O:Import | Ctrl+S:Save | Ctrl+E:Export | Del:Delete | Q:Quit";
        int helpX = width - help.length() - 2;
        if (helpX > 20) {
            screen.putString(helpX, 0, help, TextCharacter.DEFAULT);
        }
    }

    private void renderMediaPanel(int x, int y, int width, int height) {
        // Border
        drawBox(x, y, width, height, "Media Library");
        
        List<MediaAsset> assets = project.getMediaAssets();
        if (assets.isEmpty()) {
            screen.putString(x + 2, y + 2, "[No media - Press Ctrl+O to import]", TextCharacter.DEFAULT);
            return;
        }
        
        int startY = y + 2;
        int displayCount = Math.min(assets.size(), height - 4);
        int scrollOffset = Math.max(0, selectedMediaIndex - (displayCount / 2));
        
        for (int i = 0; i < displayCount && i + scrollOffset < assets.size(); i++) {
            MediaAsset asset = assets.get(i + scrollOffset);
            boolean isSelected = (i + scrollOffset) == selectedMediaIndex;
            
            String line = String.format("%-30s %s", 
                truncate(asset.getPath().substring(Math.max(0, asset.getPath().length() - 28)), 28),
                formatDuration(asset.getDuration()));
            
            if (isSelected) {
                screen.putString(x + 2, startY + i, ">" + line, TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.GREEN));
            } else {
                screen.putString(x + 2, startY + i, " " + line, TextCharacter.DEFAULT);
            }
        }
    }

    private void renderTimelinePanel(int x, int y, int width, int height) {
        drawBox(x, y, width, height, "Timeline");
        
        Timeline timeline = project.getTimeline();
        int trackY = y + 2;
        
        // Time ruler
        StringBuilder ruler = new StringBuilder();
        ruler.append("Time: ");
        ruler.append(formatDuration(playheadPosition));
        ruler.append(" | Total: ");
        ruler.append(formatDuration(calculateTotalDuration()));
        screen.putString(x + 2, trackY, ruler.toString(), TextCharacter.DEFAULT);
        trackY += 2;
        
        // Render tracks
        for (Track track : timeline.getTracks()) {
            String trackLabel = track.getType() == TrackType.VIDEO ? "[V]" : "[A]";
            screen.putString(x + 2, trackY, trackLabel, TextCharacter.DEFAULT.fromForeground(
                track.getType() == TrackType.VIDEO ? 
                    com.googlecode.lanterna.TextColor.ANSI.BLUE : 
                    com.googlecode.lanterna.TextColor.ANSI.YELLOW));
            
            // ASCII timeline visualization
            int timelineStart = x + 8;
            int timelineWidth = width - 12;
            
            // Draw timeline base
            StringBuilder timelineLine = new StringBuilder();
            for (int i = 0; i < timelineWidth; i++) {
                timelineLine.append("─");
            }
            screen.putString(timelineStart, trackY, timelineLine.toString(), 
                TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.BRIGHT_BLACK));
            
            // Draw clips
            Duration totalDuration = calculateTotalDuration();
            if (!totalDuration.isZero()) {
                for (Clip clip : track.getClips()) {
                    int clipStart = (int) ((clip.getTimelinePosition().toMillis() * timelineWidth) / 
                                          totalDuration.toMillis());
                    int clipLength = (int) ((clip.getDuration().toMillis() * timelineWidth) / 
                                           totalDuration.toMillis());
                    clipLength = Math.max(1, clipLength);
                    
                    String clipChar = clip.getType() != null ? "█" : "▓";
                    for (int i = 0; i < clipLength && clipStart + i < timelineWidth; i++) {
                        screen.putString(timelineStart + clipStart + i, trackY, clipChar,
                            TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.CYAN));
                    }
                }
            }
            
            // Draw playhead
            if (!totalDuration.isZero()) {
                int playheadX = (int) ((playheadPosition.toMillis() * timelineWidth) / 
                                      totalDuration.toMillis());
                if (playheadX >= 0 && playheadX < timelineWidth) {
                    screen.putString(timelineStart + playheadX, trackY, "│",
                        TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.RED));
                }
            }
            
            trackY++;
        }
        
        // Controls hint
        screen.putString(x + 2, y + height - 2, "←→:Move playhead | S:Split | +:Add clip", 
            TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.BRIGHT_BLACK));
    }

    private void renderInspectorPanel(int x, int y, int width, int height) {
        drawBox(x, y, width, height, "Inspector");
        
        if (selectedPanel == 0 && !project.getMediaAssets().isEmpty()) {
            // Show selected media info
            MediaAsset asset = project.getMediaAssets().get(
                Math.min(selectedMediaIndex, project.getMediaAssets().size() - 1));
            
            int row = y + 2;
            screen.putString(x + 2, row++, "Selected Media:", 
                TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.GREEN));
            screen.putString(x + 2, row++, "Path: " + truncate(asset.getPath(), width - 10), 
                TextCharacter.DEFAULT);
            screen.putString(x + 2, row++, "Duration: " + formatDuration(asset.getDuration()), 
                TextCharacter.DEFAULT);
            
            Object widthObj = asset.getMetadataValue("width");
            Object heightObj = asset.getMetadataValue("height");
            if (widthObj != null && heightObj != null) {
                screen.putString(x + 2, row++, "Resolution: " + widthObj + "x" + heightObj, 
                    TextCharacter.DEFAULT);
            }
            
            Object codecObj = asset.getMetadataValue("videoCodec");
            if (codecObj != null) {
                screen.putString(x + 2, row++, "Codec: " + codecObj, TextCharacter.DEFAULT);
            }
            
        } else if (selectedPanel == 1) {
            // Show export settings
            int row = y + 2;
            screen.putString(x + 2, row++, "Export Settings:", 
                TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.GREEN));
            
            ExportSettings settings = project.getSettings();
            screen.putString(x + 2, row++, "Format: " + settings.getFormat(), TextCharacter.DEFAULT);
            screen.putString(x + 2, row++, "Preset: " + settings.getPreset(), TextCharacter.DEFAULT);
            screen.putString(x + 2, row++, "Resolution: " + settings.getWidth() + "x" + settings.getHeight(), 
                TextCharacter.DEFAULT);
            screen.putString(x + 2, row++, "Bitrate: " + (settings.getBitrate() / 1000000) + " Mbps", 
                TextCharacter.DEFAULT);
            
            row++;
            screen.putString(x + 2, row++, "Press Ctrl+E to export", 
                TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.YELLOW));
        }
    }

    private void renderStatusBar(int width, int height) {
        // Status line
        String status = "Status: " + statusMessage;
        if (!lastProgress.isEmpty()) {
            status += " | " + lastProgress;
        }
        
        screen.fillRect(0, height - 2, width, 1, ' ', 
            TextCharacter.DEFAULT.fromBackground(com.googlecode.lanterna.TextColor.ANSI.BRIGHT_BLACK));
        screen.putString(2, height - 2, truncate(status, width - 4), TextCharacter.DEFAULT);
        
        // Panel selector
        String[] panelNames = {"[1] Media", "[2] Timeline", "[3] Inspector"};
        int panelX = (width - 30) / 2;
        for (int i = 0; i < panelNames.length; i++) {
            if (i == selectedPanel) {
                screen.putString(panelX + i * 15, height - 1, panelNames[i], 
                    TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.GREEN));
            } else {
                screen.putString(panelX + i * 15, height - 1, panelNames[i], TextCharacter.DEFAULT);
            }
        }
    }

    private boolean handleInput(KeyStroke key) {
        if (isExporting.get()) {
            // Only allow cancel during export
            if (key.getKeyType() == KeyType.Escape) {
                exportCommand.cancel();
                isExporting.set(false);
                statusMessage = "Export cancelled";
            }
            return true;
        }
        
        // Handle modifier keys
        if (key.getModifiers() != null && key.getModifiers().contains(com.googlecode.lanterna.input.Modifier.CTRL)) {
            switch (key.getKeyType()) {
                case Character:
                    char c = key.getCharacter();
                    if (c == 'o' || c == 'O') {
                        importMedia();
                        return true;
                    } else if (c == 's' || c == 'S') {
                        saveProject();
                        return true;
                    } else if (c == 'e' || c == 'E') {
                        exportProject();
                        return true;
                    } else if (c == 'r' || c == 'R') {
                        refreshMetadata();
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        
        // Regular keys
        switch (key.getKeyType()) {
            case Escape:
            case Character:
                if (key.getCharacter() == 'q' || key.getCharacter() == 'Q') {
                    return false;
                }
                break;
            default:
                break;
        }
        
        // Navigation
        switch (key.getKeyType()) {
            case ArrowLeft:
                if (selectedPanel > 0) selectedPanel--;
                break;
            case ArrowRight:
                if (selectedPanel < 2) selectedPanel++;
                break;
            case ArrowUp:
                if (selectedPanel == 0 && selectedMediaIndex > 0) selectedMediaIndex--;
                if (selectedPanel == 1 && selectedClipIndex > 0) selectedClipIndex--;
                break;
            case ArrowDown:
                if (selectedPanel == 0 && selectedMediaIndex < project.getMediaAssets().size() - 1) 
                    selectedMediaIndex++;
                break;
            case Delete:
                deleteSelectedClip();
                break;
            case Enter:
                // Add selected media to timeline
                addSelectedMediaToTimeline();
                break;
            default:
                // Character keys for hotkeys
                if (key.getKeyType() == KeyType.Character) {
                    char c = key.getCharacter();
                    if (c == 's' || c == 'S') {
                        splitClipAtPlayhead();
                    } else if (c == '+' || c == '=') {
                        addSelectedMediaToTimeline();
                    } else if (c == ' ') {
                        // Fake play toggle
                        playheadPosition = Duration.ZERO;
                        statusMessage = "Playback reset";
                    }
                }
                break;
        }
        
        return true;
    }

    private void importMedia() {
        statusMessage = "Enter file path to import (simulated)";
        // In a real implementation, this would open a file dialog or prompt
        // For MVP, we'll just show a message
        lastProgress = "File import requires CLI argument in MVP";
    }

    public void importFile(String filePath) {
        Path path = Path.of(filePath);
        if (!path.toFile().exists()) {
            statusMessage = "File not found: " + filePath;
            return;
        }
        
        CompletableFuture<MediaAsset> future = importCommand.execute(project, path);
        future.thenAccept(asset -> {
            statusMessage = "Imported: " + asset.getPath();
        }).exceptionally(ex -> {
            statusMessage = "Import failed: " + ex.getMessage();
            return null;
        });
    }

    private void addSelectedMediaToTimeline() {
        if (project.getMediaAssets().isEmpty()) {
            statusMessage = "No media to add";
            return;
        }
        
        MediaAsset asset = project.getMediaAssets().get(
            Math.min(selectedMediaIndex, project.getMediaAssets().size() - 1));
        
        try {
            importCommand.addClipToTimeline(
                project,
                asset.getId(),
                Duration.ZERO,
                asset.getDuration(),
                calculateTotalDuration(),
                TrackType.VIDEO
            );
            statusMessage = "Added clip to timeline";
        } catch (Exception e) {
            statusMessage = "Failed to add clip: " + e.getMessage();
        }
    }

    private void deleteSelectedClip() {
        if (selectedPanel == 1) {
            // Delete from timeline
            Track videoTrack = project.getTimeline().getVideoTrack();
            if (selectedClipIndex < videoTrack.getClipCount()) {
                Clip clip = videoTrack.getClips().get(selectedClipIndex);
                videoTrack.removeClip(clip);
                statusMessage = "Clip deleted";
            }
        } else if (selectedPanel == 0) {
            // Delete from library
            if (!project.getMediaAssets().isEmpty()) {
                MediaAsset asset = project.getMediaAssets().get(selectedMediaIndex);
                project.removeMediaAsset(asset);
                if (selectedMediaIndex > 0) selectedMediaIndex--;
                statusMessage = "Media removed";
            }
        }
    }

    private void splitClipAtPlayhead() {
        statusMessage = "Split not implemented in MVP";
    }

    private void saveProject() {
        try {
            Path path = Path.of("project.json");
            projectRepository.save(project, path);
            statusMessage = "Project saved to project.json";
        } catch (Exception e) {
            statusMessage = "Save failed: " + e.getMessage();
        }
    }

    private void exportProject() {
        if (project.getTimeline().getVideoTrack().getClipCount() == 0) {
            statusMessage = "No clips to export";
            return;
        }
        
        isExporting.set(true);
        project.getSettings().setOutputPath("output.mp4");
        
        CompletableFuture<FfmpegJobRunner.FfmpegResult> future = 
            exportCommand.execute(project, progress -> {
                lastProgress = progress;
            });
        
        future.thenAccept(result -> {
            isExporting.set(false);
            if (result.isSuccess()) {
                statusMessage = "Export completed: output.mp4";
            } else {
                statusMessage = "Export failed: " + result.getError();
            }
            lastProgress = "";
        }).exceptionally(ex -> {
            isExporting.set(false);
            statusMessage = "Export error: " + ex.getMessage();
            lastProgress = "";
            return null;
        });
        
        statusMessage = "Exporting...";
    }

    private void refreshMetadata() {
        statusMessage = "Metadata refreshed";
    }

    private Duration calculateTotalDuration() {
        Duration total = Duration.ZERO;
        for (Track track : project.getTimeline().getTracks()) {
            for (Clip clip : track.getClips()) {
                Duration clipEnd = clip.getTimelinePosition().plus(clip.getDuration());
                if (clipEnd.compareTo(total) > 0) {
                    total = clipEnd;
                }
            }
        }
        return total;
    }

    private void drawBox(int x, int y, int width, int height, String title) {
        // Top border with title
        screen.putString(x, y, "┌", TextCharacter.DEFAULT);
        StringBuilder top = new StringBuilder();
        for (int i = 0; i < width - 2; i++) {
            top.append("─");
        }
        screen.putString(x + 1, y, top.toString(), TextCharacter.DEFAULT);
        screen.putString(x + width - 1, y, "┐", TextCharacter.DEFAULT);
        
        // Title
        if (title != null && !title.isEmpty()) {
            screen.putString(x + 2, y, " " + title + " ", 
                TextCharacter.DEFAULT.fromForeground(com.googlecode.lanterna.TextColor.ANSI.GREEN));
        }
        
        // Side borders
        for (int i = 1; i < height - 1; i++) {
            screen.putString(x, y + i, "│", TextCharacter.DEFAULT);
            screen.putString(x + width - 1, y + i, "│", TextCharacter.DEFAULT);
        }
        
        // Bottom border
        screen.putString(x, y + height - 1, "└", TextCharacter.DEFAULT);
        StringBuilder bottom = new StringBuilder();
        for (int i = 0; i < width - 2; i++) {
            bottom.append("─");
        }
        screen.putString(x + 1, y + height - 1, bottom.toString(), TextCharacter.DEFAULT);
        screen.putString(x + width - 1, y + height - 1, "┘", TextCharacter.DEFAULT);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }

    private void cleanup() {
        try {
            if (screen != null) {
                screen.close();
            }
        } catch (IOException e) {
            log.error("Error closing screen", e);
        }
    }
}
