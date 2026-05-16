package com.videoeditor.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A clip on the timeline, referencing a media asset with timing information.
 */
public class Clip {
    private final EntityId id;
    private final EntityId assetId;
    private Duration startTime;      // Start time within the source asset
    private Duration endTime;        // End time within the source asset
    private Duration timelinePosition; // Position on the timeline
    private List<Effect> effects;

    public Clip(EntityId assetId, Duration startTime, Duration endTime, Duration timelinePosition) {
        this.id = EntityId.generate();
        this.assetId = assetId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timelinePosition = timelinePosition;
        this.effects = new ArrayList<>();
    }

    // For JSON deserialization
    public Clip(EntityId id, EntityId assetId, Duration startTime, Duration endTime, 
                Duration timelinePosition, List<Effect> effects) {
        this.id = id;
        this.assetId = assetId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timelinePosition = timelinePosition;
        this.effects = effects != null ? new ArrayList<>(effects) : new ArrayList<>();
    }

    public EntityId getId() {
        return id;
    }

    public EntityId getAssetId() {
        return assetId;
    }

    public Duration getStartTime() {
        return startTime;
    }

    public void setStartTime(Duration startTime) {
        this.startTime = startTime;
    }

    public Duration getEndTime() {
        return endTime;
    }

    public void setEndTime(Duration endTime) {
        this.endTime = endTime;
    }

    public Duration getTimelinePosition() {
        return timelinePosition;
    }

    public void setTimelinePosition(Duration timelinePosition) {
        this.timelinePosition = timelinePosition;
    }

    public Duration getDuration() {
        return endTime.minus(startTime);
    }

    public List<Effect> getEffects() {
        return new ArrayList<>(effects);
    }

    public void addEffect(Effect effect) {
        effects.add(effect);
    }

    public void removeEffect(int index) {
        if (index >= 0 && index < effects.size()) {
            effects.remove(index);
        }
    }

    public void clearEffects() {
        effects.clear();
    }

    @Override
    public String toString() {
        return "Clip{id=" + id + ", assetId=" + assetId + 
               ", start=" + startTime + ", end=" + endTime + 
               ", pos=" + timelinePosition + "}";
    }
}
