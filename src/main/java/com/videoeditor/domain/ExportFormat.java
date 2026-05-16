package com.videoeditor.domain;

/**
 * Export format for the video.
 */
public enum ExportFormat {
    MP4("mp4", "libx264", "aac"),
    MKV("mkv", "libx264", "aac"),
    WEBM("webm", "libvpx-vp9", "libvorbis");

    private final String extension;
    private final String videoCodec;
    private final String audioCodec;

    ExportFormat(String extension, String videoCodec, String audioCodec) {
        this.extension = extension;
        this.videoCodec = videoCodec;
        this.audioCodec = audioCodec;
    }

    public String getExtension() {
        return extension;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public String getAudioCodec() {
        return audioCodec;
    }
}
