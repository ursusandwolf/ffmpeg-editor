package com.videoeditor.infrastructure.ffmpeg;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata extracted from media files using ffprobe.
 */
public class MediaMetadata {
    private Duration duration;
    private int width;
    private int height;
    private String videoCodec;
    private String audioCodec;
    private long fileSize;
    private double frameRate;
    private Map<String, String> tags;

    public MediaMetadata() {
        this.duration = Duration.ZERO;
        this.width = 0;
        this.height = 0;
        this.tags = new HashMap<>();
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public double getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(double frameRate) {
        this.frameRate = frameRate;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags != null ? new HashMap<>(tags) : new HashMap<>();
    }

    public void addTag(String key, String value) {
        tags.put(key, value);
    }

    @Override
    public String toString() {
        return "MediaMetadata{duration=" + duration + ", resolution=" + width + "x" + height + 
               ", videoCodec=" + videoCodec + ", audioCodec=" + audioCodec + "}";
    }
}
