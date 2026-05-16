package com.videoeditor.domain;

import java.time.Duration;

/**
 * Settings for video export.
 */
public class ExportSettings {
    private ExportFormat format;
    private ExportPreset preset;
    private int width;
    private int height;
    private int bitrate;
    private String outputPath;

    public ExportSettings() {
        this.format = ExportFormat.MP4;
        this.preset = ExportPreset.BALANCED;
        this.width = 1920;
        this.height = 1080;
        this.bitrate = 5000000; // 5 Mbps
    }

    public ExportFormat getFormat() {
        return format;
    }

    public void setFormat(ExportFormat format) {
        this.format = format;
    }

    public ExportPreset getPreset() {
        return preset;
    }

    public void setPreset(ExportPreset preset) {
        this.preset = preset;
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

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public String toString() {
        return "ExportSettings{format=" + format + ", preset=" + preset + 
               ", resolution=" + width + "x" + height + 
               ", bitrate=" + bitrate + "}";
    }
}
