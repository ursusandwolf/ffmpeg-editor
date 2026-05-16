package com.videoeditor.domain;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Media asset representing an imported video/audio file.
 */
public class MediaAsset {
    private final EntityId id;
    private final String path;
    private final Duration duration;
    private final Map<String, Object> metadata;

    public MediaAsset(String path, Duration duration) {
        this.id = EntityId.generate();
        this.path = path;
        this.duration = duration;
        this.metadata = new HashMap<>();
    }

    // For JSON deserialization
    public MediaAsset(EntityId id, String path, Duration duration, Map<String, Object> metadata) {
        this.id = id;
        this.path = path;
        this.duration = duration;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public EntityId getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public Duration getDuration() {
        return duration;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    @Override
    public String toString() {
        return "MediaAsset{id=" + id + ", path='" + path + "', duration=" + duration + "}";
    }
}
