package com.videoeditor.application.commands;

import com.videoeditor.domain.*;
import com.videoeditor.infrastructure.ffmpeg.FfprobeClient;
import com.videoeditor.infrastructure.ffmpeg.MediaMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Use case for importing media files into a project.
 */
public class ImportMediaCommand {
    private static final Logger log = LoggerFactory.getLogger(ImportMediaCommand.class);
    private final FfprobeClient ffprobeClient;

    public ImportMediaCommand() {
        this(new FfprobeClient());
    }

    public ImportMediaCommand(FfprobeClient ffprobeClient) {
        this.ffprobeClient = ffprobeClient;
    }

    /**
     * Import a media file and add it to the project.
     * Returns CompletableFuture with the created MediaAsset.
     */
    public CompletableFuture<MediaAsset> execute(Project project, Path filePath) {
        log.info("Importing media file: {}", filePath);
        
        return ffprobeClient.probeAsync(filePath.toString())
            .thenApply(metadata -> {
                if (metadata.getDuration().isZero()) {
                    throw new ImportException("Could not determine media duration: " + filePath);
                }
                
                MediaAsset asset = new MediaAsset(
                    filePath.toString(),
                    metadata.getDuration()
                );
                
                // Add metadata
                asset.addMetadata("width", metadata.getWidth());
                asset.addMetadata("height", metadata.getHeight());
                asset.addMetadata("videoCodec", metadata.getVideoCodec());
                asset.addMetadata("audioCodec", metadata.getAudioCodec());
                asset.addMetadata("fileSize", metadata.getFileSize());
                asset.addMetadata("frameRate", metadata.getFrameRate());
                
                project.addMediaAsset(asset);
                log.info("Successfully imported: {} (duration: {})", filePath, metadata.getDuration());
                
                return asset;
            });
    }

    /**
     * Add a clip from an existing asset to the timeline.
     */
    public Clip addClipToTimeline(Project project, EntityId assetId, 
                                   Duration startTime, Duration endTime,
                                   Duration timelinePosition, TrackType trackType) {
        MediaAsset asset = project.getMediaAssetById(assetId);
        if (asset == null) {
            throw new ImportException("Asset not found: " + assetId);
        }
        
        // Validate timing
        if (startTime.isNegative() || endTime.isNegative() || 
            endTime.compareTo(startTime) <= 0) {
            throw new ImportException("Invalid clip timing");
        }
        
        if (endTime.compareTo(asset.getDuration()) > 0) {
            throw new ImportException("End time exceeds asset duration");
        }
        
        Clip clip = new Clip(assetId, startTime, endTime, timelinePosition);
        
        Track track = trackType == TrackType.VIDEO ? 
            project.getTimeline().getVideoTrack() : 
            project.getTimeline().getAudioTrack();
        
        track.addClip(clip);
        log.info("Added clip to timeline: {}", clip);
        
        return clip;
    }

    /**
     * Remove a clip from the timeline.
     */
    public void removeClipFromTimeline(Project project, EntityId clipId) {
        Timeline timeline = project.getTimeline();
        
        for (Track track : timeline.getTracks()) {
            Clip clip = track.getClipById(clipId);
            if (clip != null) {
                track.removeClip(clip);
                log.info("Removed clip from timeline: {}", clipId);
                return;
            }
        }
        
        throw new ImportException("Clip not found: " + clipId);
    }

    /**
     * Split a clip at a specific position.
     */
    public void splitClip(Project project, EntityId clipId, Duration splitPosition) {
        Timeline timeline = project.getTimeline();
        
        for (Track track : timeline.getTracks()) {
            Clip clip = track.getClipById(clipId);
            if (clip != null) {
                // Validate split position
                Duration clipStart = clip.getTimelinePosition();
                Duration clipEnd = clipStart.plus(clip.getDuration());
                
                if (splitPosition.compareTo(clipStart) <= 0 || 
                    splitPosition.compareTo(clipEnd) >= 0) {
                    throw new ImportException("Invalid split position");
                }
                
                // Calculate split point within source
                Duration offsetInSource = splitPosition.minus(clipStart);
                Duration newStartTime = clip.getStartTime().plus(offsetInSource);
                
                // Create second clip
                Clip secondClip = new Clip(
                    clip.getAssetId(),
                    newStartTime,
                    clip.getEndTime(),
                    splitPosition
                );
                
                // Modify first clip
                clip.setEndTime(newStartTime);
                
                // Add second clip after the first (must mutate track, not a defensive copy)
                int index = track.getClipIndex(clip);
                track.insertClip(index + 1, secondClip);
                
                log.info("Split clip {} at {}", clipId, splitPosition);
                return;
            }
        }
        
        throw new ImportException("Clip not found: " + clipId);
    }

    /**
     * Trim a clip's in/out points.
     */
    public void trimClip(Project project, EntityId clipId, 
                         Duration newStart, Duration newEnd) {
        Timeline timeline = project.getTimeline();
        
        for (Track track : timeline.getTracks()) {
            Clip clip = track.getClipById(clipId);
            if (clip != null) {
                MediaAsset asset = project.getMediaAssetById(clip.getAssetId());
                if (asset == null) {
                    throw new ImportException("Asset not found");
                }
                
                if (newStart.isNegative() || newEnd.isNegative() ||
                    newEnd.compareTo(newStart) <= 0 ||
                    newEnd.compareTo(asset.getDuration()) > 0) {
                    throw new ImportException("Invalid trim values");
                }
                
                clip.setStartTime(newStart);
                clip.setEndTime(newEnd);
                
                log.info("Trimmed clip {} to {} - {}", clipId, newStart, newEnd);
                return;
            }
        }
        
        throw new ImportException("Clip not found: " + clipId);
    }

    /**
     * Exception thrown when import operations fail.
     */
    public static class ImportException extends RuntimeException {
        public ImportException(String message) {
            super(message);
        }
        
        public ImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
