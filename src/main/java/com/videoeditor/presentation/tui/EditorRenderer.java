package com.videoeditor.presentation.tui;

import com.googlecode.lanterna.TextColor;
import com.videoeditor.domain.Clip;
import com.videoeditor.domain.ExportSettings;
import com.videoeditor.domain.MediaAsset;
import com.videoeditor.domain.Project;
import com.videoeditor.domain.Timeline;
import com.videoeditor.domain.Track;
import com.videoeditor.domain.TrackType;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Draws the editor layout from project + UI state.
 */
final class EditorRenderer {
    private final ScreenCanvas canvas;
    private final Project project;
    private final EditorUiState ui;

    EditorRenderer(ScreenCanvas canvas, Project project, EditorUiState ui) {
        this.canvas = canvas;
        this.project = project;
        this.ui = ui;
    }

    void render() throws IOException {
        canvas.screen().clear();

        int height = canvas.screen().getTerminalSize().getRows();
        int width = canvas.screen().getTerminalSize().getColumns();
        int panelWidth = (width - 4) / 3;
        int contentHeight = height - 8;

        renderHeader(width);
        renderMediaPanel(2, 2, panelWidth, contentHeight);
        renderTimelinePanel(panelWidth + 4, 2, panelWidth * 2 - 2, contentHeight / 2);
        renderInspectorPanel(panelWidth + 4, contentHeight / 2 + 3, panelWidth * 2 - 2, contentHeight / 2);
        renderStatusBar(width, height);

        canvas.screen().refresh();
    }

    private void renderHeader(int width) {
        String title = "═══ Video Editor MVP ═══";
        int padding = (width - title.length()) / 2;
        canvas.put(padding, 0, title);
        canvas.put(2, 0, "Project: " + project.getName());

        String help = "Ctrl+O:Import | Ctrl+S:Save | Ctrl+E:Export | Del:Delete | Q:Quit";
        int helpX = width - help.length() - 2;
        if (helpX > 20) {
            canvas.put(helpX, 0, help);
        }
    }

    private void renderMediaPanel(int x, int y, int width, int height) {
        canvas.drawBox(x, y, width, height, "Media Library");

        List<MediaAsset> assets = project.getMediaAssets();
        if (assets.isEmpty()) {
            canvas.put(x + 2, y + 2, "[No media - Press Ctrl+O to import]");
            return;
        }

        int startY = y + 2;
        int displayCount = Math.min(assets.size(), height - 4);
        int scrollOffset = Math.max(0, ui.getSelectedMediaIndex() - (displayCount / 2));

        for (int i = 0; i < displayCount && i + scrollOffset < assets.size(); i++) {
            MediaAsset asset = assets.get(i + scrollOffset);
            boolean selected = (i + scrollOffset) == ui.getSelectedMediaIndex();
            String line = String.format("%-30s %s",
                    TuiText.tailPath(asset.getPath(), 28),
                    TuiText.formatDuration(asset.getDuration()));

            if (selected) {
                canvas.put(x + 2, startY + i, ">" + line, TextColor.ANSI.GREEN);
            } else {
                canvas.put(x + 2, startY + i, " " + line);
            }
        }
    }

    private void renderTimelinePanel(int x, int y, int width, int height) {
        canvas.drawBox(x, y, width, height, "Timeline");

        Timeline timeline = project.getTimeline();
        int trackY = y + 2;
        Duration totalDuration = TimelineMetrics.totalDuration(project);

        canvas.put(x + 2, trackY, "Time: " + TuiText.formatDuration(ui.getPlayheadPosition())
                + " | Total: " + TuiText.formatDuration(totalDuration));
        trackY += 2;

        for (Track track : timeline.getTracks()) {
            String trackLabel = track.getType() == TrackType.VIDEO ? "[V]" : "[A]";
            TextColor trackColor = track.getType() == TrackType.VIDEO
                    ? TextColor.ANSI.BLUE
                    : TextColor.ANSI.YELLOW;
            canvas.put(x + 2, trackY, trackLabel, trackColor);

            int timelineStart = x + 8;
            int timelineWidth = width - 12;
            canvas.put(timelineStart, trackY, "─".repeat(Math.max(0, timelineWidth)), TextColor.ANSI.WHITE);

            if (!totalDuration.isZero()) {
                for (Clip clip : track.getClips()) {
                    int clipStart = (int) ((clip.getTimelinePosition().toMillis() * timelineWidth)
                            / totalDuration.toMillis());
                    int clipLength = Math.max(1, (int) ((clip.getDuration().toMillis() * timelineWidth)
                            / totalDuration.toMillis()));
                    String clipChar = clip.getType() != null ? "█" : "▓";
                    for (int i = 0; i < clipLength && clipStart + i < timelineWidth; i++) {
                        canvas.put(timelineStart + clipStart + i, trackY, clipChar, TextColor.ANSI.CYAN);
                    }
                }

                int playheadX = (int) ((ui.getPlayheadPosition().toMillis() * timelineWidth)
                        / totalDuration.toMillis());
                if (playheadX >= 0 && playheadX < timelineWidth) {
                    canvas.put(timelineStart + playheadX, trackY, "│", TextColor.ANSI.RED);
                }
            }

            trackY++;
        }

        canvas.put(x + 2, y + height - 2, "←→:Move playhead | S:Split | +:Add clip", TextColor.ANSI.WHITE);
    }

    private void renderInspectorPanel(int x, int y, int width, int height) {
        canvas.drawBox(x, y, width, height, "Inspector");

        if (ui.getSelectedPanel() == PanelId.MEDIA && !project.getMediaAssets().isEmpty()) {
            MediaAsset asset = project.getMediaAssets().get(
                    Math.min(ui.getSelectedMediaIndex(), project.getMediaAssets().size() - 1));

            int row = y + 2;
            canvas.put(x + 2, row++, "Selected Media:", TextColor.ANSI.GREEN);
            canvas.put(x + 2, row++, "Path: " + TuiText.truncate(asset.getPath(), width - 10));
            canvas.put(x + 2, row++, "Duration: " + TuiText.formatDuration(asset.getDuration()));

            Object widthObj = asset.getMetadataValue("width");
            Object heightObj = asset.getMetadataValue("height");
            if (widthObj != null && heightObj != null) {
                canvas.put(x + 2, row++, "Resolution: " + widthObj + "x" + heightObj);
            }

            Object codecObj = asset.getMetadataValue("videoCodec");
            if (codecObj != null) {
                canvas.put(x + 2, row, "Codec: " + codecObj);
            }
            return;
        }

        if (ui.getSelectedPanel() == PanelId.TIMELINE) {
            int row = y + 2;
            canvas.put(x + 2, row++, "Export Settings:", TextColor.ANSI.GREEN);

            ExportSettings settings = project.getSettings();
            canvas.put(x + 2, row++, "Format: " + settings.getFormat());
            canvas.put(x + 2, row++, "Preset: " + settings.getPreset());
            canvas.put(x + 2, row++, "Resolution: " + settings.getWidth() + "x" + settings.getHeight());
            canvas.put(x + 2, row++, "Bitrate: " + (settings.getBitrate() / 1_000_000) + " Mbps");
            row++;
            canvas.put(x + 2, row, "Press Ctrl+E to export", TextColor.ANSI.YELLOW);
        }
    }

    private void renderStatusBar(int width, int height) {
        String status = "Status: " + ui.getStatusMessage();
        if (!ui.getLastProgress().isEmpty()) {
            status += " | " + ui.getLastProgress();
        }

        canvas.put(0, height - 2, " ".repeat(Math.max(0, width)));
        canvas.put(2, height - 2, TuiText.truncate(status, width - 4));

        String[] panelNames = {"[1] Media", "[2] Timeline", "[3] Inspector"};
        int panelX = (width - 30) / 2;
        for (int i = 0; i < panelNames.length; i++) {
            if (i == ui.getSelectedPanel().index()) {
                canvas.put(panelX + i * 15, height - 1, panelNames[i], TextColor.ANSI.GREEN);
            } else {
                canvas.put(panelX + i * 15, height - 1, panelNames[i]);
            }
        }
    }
}
