package com.videoeditor.application.commands;

import com.videoeditor.domain.*;
import com.videoeditor.infrastructure.ffmpeg.FfmpegCommandBuilder;
import com.videoeditor.infrastructure.ffmpeg.FfmpegJobRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Use case for exporting a project to video file.
 */
public class ExportProjectCommand {
    private static final Logger log = LoggerFactory.getLogger(ExportProjectCommand.class);
    private final FfmpegCommandBuilder commandBuilder;
    private final FfmpegJobRunner jobRunner;

    public ExportProjectCommand() {
        this(new FfmpegCommandBuilder(), new FfmpegJobRunner());
    }

    public ExportProjectCommand(FfmpegCommandBuilder commandBuilder, FfmpegJobRunner jobRunner) {
        this.commandBuilder = commandBuilder;
        this.jobRunner = jobRunner;
    }

    /**
     * Export project asynchronously with progress callback.
     */
    public CompletableFuture<FfmpegJobRunner.FfmpegResult> execute(Project project, 
                                                                    Consumer<String> progressCallback) {
        log.info("Starting export for project: {}", project.getName());
        
        // Validate project has clips
        Timeline timeline = project.getTimeline();
        boolean hasClips = false;
        for (Track track : timeline.getTracks()) {
            if (track.getClipCount() > 0) {
                hasClips = true;
                break;
            }
        }
        
        if (!hasClips) {
            CompletableFuture<FfmpegJobRunner.FfmpegResult> future = new CompletableFuture<>();
            future.completeExceptionally(new ExportException("No clips in timeline to export"));
            return future;
        }
        
        // Build FFmpeg command
        List<String> command = commandBuilder.buildExportCommand(
            project, 
            project.getSettings()
        );
        
        // Run export
        return jobRunner.runAsync(command, progressCallback);
    }

    /**
     * Preview the FFmpeg command that would be executed.
     */
    public String previewCommand(Project project) {
        List<String> command = commandBuilder.buildExportCommand(
            project,
            project.getSettings()
        );
        return String.join(" ", command);
    }

    /**
     * Cancel ongoing export.
     */
    public void cancel() {
        log.info("Cancelling export");
        jobRunner.cancel();
    }

    /**
     * Check if FFmpeg is available.
     */
    public boolean isFfmpegAvailable() {
        return jobRunner.isAvailable();
    }

    /**
     * Get FFmpeg version.
     */
    public String getFfmpegVersion() {
        return jobRunner.getVersion();
    }

    /**
     * Exception thrown when export operations fail.
     */
    public static class ExportException extends RuntimeException {
        public ExportException(String message) {
            super(message);
        }
        
        public ExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
