package com.videoeditor.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Main project entity containing all media assets and timeline.
 */
public class Project {
    private static final String SCHEMA_VERSION = "1.0";
    
    private EntityId id;
    private String name;
    private List<MediaAsset> mediaAssets;
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

    // For JSON deserialization
    public Project(EntityId id, String name, List<MediaAsset> mediaAssets, 
                   Timeline timeline, ExportSettings settings) {
        this.id = id;
        this.name = name;
        this.mediaAssets = mediaAssets != null ? new ArrayList<>(mediaAssets) : new ArrayList<>();
        this.timeline = timeline != null ? timeline : new Timeline();
        this.settings = settings != null ? settings : new ExportSettings();
        this.totalDuration = Duration.ZERO;
    }

    public EntityId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MediaAsset> getMediaAssets() {
        return new ArrayList<>(mediaAssets);
    }

    public void addMediaAsset(MediaAsset asset) {
        mediaAssets.add(asset);
    }

    public void removeMediaAsset(MediaAsset asset) {
        mediaAssets.remove(asset);
    }

    public MediaAsset getMediaAssetById(EntityId assetId) {
        for (MediaAsset asset : mediaAssets) {
            if (asset.getId().equals(assetId)) {
                return asset;
            }
        }
        return null;
    }

    public int getMediaAssetCount() {
        return mediaAssets.size();
    }

    public Timeline getTimeline() {
        return timeline;
    }

    public void setTimeline(Timeline timeline) {
        this.timeline = timeline;
    }

    public ExportSettings getSettings() {
        return settings;
    }

    public void setSettings(ExportSettings settings) {
        this.settings = settings;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Duration totalDuration) {
        this.totalDuration = totalDuration;
    }

    public String getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + "', assets=" + mediaAssets.size() + "}";
    }
}
