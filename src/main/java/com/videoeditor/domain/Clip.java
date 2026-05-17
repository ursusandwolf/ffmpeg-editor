package com.videoeditor.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A clip on the timeline, referencing a media asset with timing information.
 */
@Getter
@Setter
@ToString
public class Clip {
    private final EntityId id;
    private final EntityId assetId;
    private Duration startTime;
    private Duration endTime;
    private Duration timelinePosition;
    private List<Effect> effects;
    private String type;

    public Clip(EntityId assetId, Duration startTime, Duration endTime, Duration timelinePosition) {
        this.id = EntityId.generate();
        this.assetId = assetId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timelinePosition = timelinePosition;
        this.effects = new ArrayList<>();
        this.type = "default";
    }

    @Builder
    public Clip(EntityId id, EntityId assetId, Duration startTime, Duration endTime, 
                Duration timelinePosition, List<Effect> effects, String type) {
        this.id = id != null ? id : EntityId.generate();
        this.assetId = assetId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timelinePosition = timelinePosition;
        this.effects = effects != null ? new ArrayList<>(effects) : new ArrayList<>();
        this.type = type != null ? type : "default";
    }

    public Duration getDuration() {
        return endTime.minus(startTime);
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
}
