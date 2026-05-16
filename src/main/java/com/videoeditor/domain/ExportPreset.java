package com.videoeditor.domain;

/**
 * Export preset for quality/speed trade-off.
 */
public enum ExportPreset {
    FAST("fast", "-preset fast", "Fast export, lower quality"),
    BALANCED("balanced", "-preset medium", "Balanced speed and quality"),
    HIGH_QUALITY("high_quality", "-preset slow -crf 18", "Best quality, slower export");

    private final String name;
    private final String ffmpegArgs;
    private final String description;

    ExportPreset(String name, String ffmpegArgs, String description) {
        this.name = name;
        this.ffmpegArgs = ffmpegArgs;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getFfmpegArgs() {
        return ffmpegArgs;
    }

    public String getDescription() {
        return description;
    }
}
