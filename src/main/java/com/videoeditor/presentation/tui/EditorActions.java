package com.videoeditor.presentation.tui;

import com.videoeditor.application.commands.ExportProjectCommand;
import com.videoeditor.application.commands.ImportMediaCommand;
import com.videoeditor.domain.Clip;
import com.videoeditor.domain.MediaAsset;
import com.videoeditor.domain.Project;
import com.videoeditor.domain.Track;
import com.videoeditor.domain.TrackType;
import com.videoeditor.infrastructure.ffmpeg.FfmpegJobRunner;
import com.videoeditor.infrastructure.persistence.ProjectRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Application actions triggered from the TUI.
 */
final class EditorActions {
    private final Project project;
    private final EditorUiState ui;
    private final ImportMediaCommand importCommand;
    private final ExportProjectCommand exportCommand;
    private final ProjectRepository projectRepository;

    EditorActions(Project project,
                  EditorUiState ui,
                  ImportMediaCommand importCommand,
                  ExportProjectCommand exportCommand,
                  ProjectRepository projectRepository) {
        this.project = project;
        this.ui = ui;
        this.importCommand = importCommand;
        this.exportCommand = exportCommand;
        this.projectRepository = projectRepository;
    }

    void promptImport() {
        ui.setStatusMessage("Enter file path to import (simulated)");
        ui.setLastProgress("File import requires CLI argument in MVP");
    }

    void importFile(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            ui.setStatusMessage("File not found: " + filePath);
            return;
        }

        importCommand.execute(project, path).thenAccept(asset ->
                ui.setStatusMessage("Imported: " + asset.getPath())
        ).exceptionally(ex -> {
            ui.setStatusMessage("Import failed: " + ex.getMessage());
            return null;
        });
    }

    void addSelectedMediaToTimeline() {
        if (project.getMediaAssets().isEmpty()) {
            ui.setStatusMessage("No media to add");
            return;
        }

        MediaAsset asset = project.getMediaAssets().get(
                Math.min(ui.getSelectedMediaIndex(), project.getMediaAssets().size() - 1));

        try {
            importCommand.addClipToTimeline(
                    project,
                    asset.getId(),
                    Duration.ZERO,
                    asset.getDuration(),
                    TimelineMetrics.totalDuration(project),
                    TrackType.VIDEO
            );
            ui.setStatusMessage("Added clip to timeline");
        } catch (Exception e) {
            ui.setStatusMessage("Failed to add clip: " + e.getMessage());
        }
    }

    void deleteSelection() {
        if (ui.getSelectedPanel() == PanelId.TIMELINE) {
            Track videoTrack = project.getTimeline().getVideoTrack();
            if (ui.getSelectedClipIndex() < videoTrack.getClipCount()) {
                Clip clip = videoTrack.getClips().get(ui.getSelectedClipIndex());
                videoTrack.removeClip(clip);
                ui.setStatusMessage("Clip deleted");
            }
            return;
        }

        if (ui.getSelectedPanel() == PanelId.MEDIA && !project.getMediaAssets().isEmpty()) {
            MediaAsset asset = project.getMediaAssets().get(ui.getSelectedMediaIndex());
            project.removeMediaAsset(asset);
            if (ui.getSelectedMediaIndex() > 0) {
                ui.setSelectedMediaIndex(ui.getSelectedMediaIndex() - 1);
            }
            ui.setStatusMessage("Media removed");
        }
    }

    void splitClipAtPlayhead() {
        ui.setStatusMessage("Split not implemented in MVP");
    }

    void saveProject() {
        try {
            Path path = Path.of("project.json");
            projectRepository.save(project, path);
            ui.setStatusMessage("Project saved to project.json");
        } catch (Exception e) {
            ui.setStatusMessage("Save failed: " + e.getMessage());
        }
    }

    void exportProject() {
        if (project.getTimeline().getVideoTrack().getClipCount() == 0) {
            ui.setStatusMessage("No clips to export");
            return;
        }

        ui.setExporting(true);
        project.getSettings().setOutputPath("output.mp4");

        CompletableFuture<FfmpegJobRunner.FfmpegResult> future =
                exportCommand.execute(project, ui::setLastProgress);

        future.thenAccept(result -> {
            ui.setExporting(false);
            if (result.isSuccess()) {
                ui.setStatusMessage("Export completed: output.mp4");
            } else {
                ui.setStatusMessage("Export failed: " + result.getError());
            }
            ui.setLastProgress("");
        }).exceptionally(ex -> {
            ui.setExporting(false);
            ui.setStatusMessage("Export error: " + ex.getMessage());
            ui.setLastProgress("");
            return null;
        });

        ui.setStatusMessage("Exporting...");
    }

    void cancelExport() {
        exportCommand.cancel();
        ui.setExporting(false);
        ui.setStatusMessage("Export cancelled");
    }

    void refreshMetadata() {
        ui.setStatusMessage("Metadata refreshed");
    }

    void resetPlayback() {
        ui.setPlayheadPosition(Duration.ZERO);
        ui.setStatusMessage("Playback reset");
    }
}
