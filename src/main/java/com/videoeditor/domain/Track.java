package com.videoeditor.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A track in the timeline containing clips.
 */
public class Track {
    private final EntityId id;
    private final TrackType type;
    private final List<Clip> clips;

    public Track(TrackType type) {
        this.id = EntityId.generate();
        this.type = type;
        this.clips = new ArrayList<>();
    }

    // For JSON deserialization
    public Track(EntityId id, TrackType type, List<Clip> clips) {
        this.id = id;
        this.type = type;
        this.clips = clips != null ? new ArrayList<>(clips) : new ArrayList<>();
    }

    public EntityId getId() {
        return id;
    }

    public TrackType getType() {
        return type;
    }

    public List<Clip> getClips() {
        return new ArrayList<>(clips);
    }

    public void addClip(Clip clip) {
        clips.add(clip);
    }

    public void insertClip(int index, Clip clip) {
        clips.add(index, clip);
    }

    public void removeClip(Clip clip) {
        clips.remove(clip);
    }

    public void removeClipById(EntityId clipId) {
        clips.removeIf(clip -> clip.getId().equals(clipId));
    }

    public Clip getClipById(EntityId clipId) {
        for (Clip clip : clips) {
            if (clip.getId().equals(clipId)) {
                return clip;
            }
        }
        return null;
    }

    public int getClipIndex(Clip clip) {
        return clips.indexOf(clip);
    }

    public void moveClip(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < clips.size() && 
            toIndex >= 0 && toIndex < clips.size()) {
            Clip clip = clips.remove(fromIndex);
            clips.add(toIndex, clip);
        }
    }

    public int getClipCount() {
        return clips.size();
    }

    @Override
    public String toString() {
        return "Track{id=" + id + ", type=" + type + ", clips=" + clips.size() + "}";
    }
}
