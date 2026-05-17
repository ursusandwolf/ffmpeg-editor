package com.videoeditor.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Main project entity containing all media assets and timeline.
 */
@Getter
@Setter
@ToString
public class Project {
    private static final String SCHEMA_VERSION = "1.0";
    
    private final EntityId id;
    private String name;
    private final List<MediaAsset> mediaAssets;
    private Timeline timeline;
    private ExportSettings settings;
    private Duration totalDuration;

    public Project() {
        this.id = EntityId.generate();
        this.name = "Untitled Project";
        this.mediaAssets = new ArrayList<>();
        this.timeline = new Timeline();
        this.settings = new ExportSettings();
        this.totalDuration = Duration.ZERO;
    }

    @Builder
    public Project(EntityId id, String name, List<MediaAsset> mediaAssets, 
                   Timeline timeline, ExportSettings settings) {
        this.id = id != null ? id : EntityId.generate();
        this.name = name != null ? name : "Untitled Project";
        this.mediaAssets = mediaAssets != null ? new ArrayList<>(mediaAssets) : new ArrayList<>();
        this.timeline = timeline != null ? timeline : new Timeline();
        this.settings = settings != null ? settings : new ExportSettings();
        this.totalDuration = Duration.ZERO;
    }

    public void addMediaAsset(MediaAsset asset) {
        mediaAssets.add(asset);
    }

    public void removeMediaAsset(MediaAsset asset) {
        mediaAssets.remove(asset);
    }

    public MediaAsset getMediaAssetById(EntityId assetId) {
        return mediaAssets.stream()
                .filter(asset -> asset.getId().equals(assetId))
                .findFirst()
                .orElse(null);
    }

    public int getMediaAssetCount() {
        return mediaAssets.size();
    }

    public String getSchemaVersion() {
        return SCHEMA_VERSION;
    }
}
