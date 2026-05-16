package com.videoeditor.infrastructure.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for interacting with ffprobe to extract media metadata.
 */
public class FfprobeClient {
    private static final Logger log = LoggerFactory.getLogger(FfprobeClient.class);
    private final String ffprobePath;
    private final ExecutorService executor;

    public FfprobeClient() {
        this("ffprobe");
    }

    public FfprobeClient(String ffprobePath) {
        this.ffprobePath = ffprobePath;
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * Probe a media file and extract metadata asynchronously.
     */
    public CompletableFuture<MediaMetadata> probeAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> probe(filePath), executor);
    }

    /**
     * Probe a media file and extract metadata synchronously.
     */
    public MediaMetadata probe(String filePath) {
        log.info("Probing media file: {}", filePath);
        
        List<String> command = new ArrayList<>();
        command.add(ffprobePath);
        command.add("-v");
        command.add("quiet");
        command.add("-print_format");
        command.add("json");
        command.add("-show_format");
        command.add("-show_streams");
        command.add(filePath);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("ffprobe failed with exit code: {}", exitCode);
                throw new FfprobeException("ffprobe failed with exit code: " + exitCode);
            }

            return parseJsonOutput(output.toString(), filePath);

        } catch (IOException e) {
            log.error("Failed to execute ffprobe", e);
            throw new FfprobeException("Failed to execute ffprobe: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FfprobeException("ffprobe interrupted", e);
        }
    }

    private MediaMetadata parseJsonOutput(String json, String filePath) {
        MediaMetadata metadata = new MediaMetadata();
        
        // Simple parsing without full JSON library for streams/format
        // Duration
        Pattern durationPattern = Pattern.compile("\"duration\"\\s*:\\s*\"([0-9.]+)\"");
        Matcher durationMatcher = durationPattern.matcher(json);
        if (durationMatcher.find()) {
            double seconds = Double.parseDouble(durationMatcher.group(1));
            metadata.setDuration(Duration.ofMillis((long) (seconds * 1000)));
        }

        // File size
        Pattern sizePattern = Pattern.compile("\"size\"\\s*:\\s*\"?([0-9]+)\"?");
        Matcher sizeMatcher = sizePattern.matcher(json);
        if (sizeMatcher.find()) {
            metadata.setFileSize(Long.parseLong(sizeMatcher.group(1)));
        }

        // Video stream info
        Pattern videoCodecPattern = Pattern.compile("\"codec_name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern widthPattern = Pattern.compile("\"width\"\\s*:\\s*([0-9]+)");
        Pattern heightPattern = Pattern.compile("\"height\"\\s*:\\s*([0-9]+)");
        Pattern frameratePattern = Pattern.compile("\"r_frame_rate\"\\s*:\\s*\"([0-9]+)/([0-9]+)\"");

        // Find video stream section
        int videoStreamIndex = json.indexOf("\"codec_type\"\\s*:\\s*\"video\"".replace("\\s*", ""));
        if (videoStreamIndex == -1) {
            // Try simpler match
            videoStreamIndex = json.indexOf("\"codec_type\":\"video\"");
        }
        
        if (videoStreamIndex >= 0) {
            String videoSection = json.substring(videoStreamIndex, 
                Math.min(videoStreamIndex + 2000, json.length()));
            
            Matcher codecMatcher = videoCodecPattern.matcher(videoSection);
            if (codecMatcher.find()) {
                metadata.setVideoCodec(codecMatcher.group(1));
            }

            Matcher widthMatcher = widthPattern.matcher(videoSection);
            if (widthMatcher.find()) {
                metadata.setWidth(Integer.parseInt(widthMatcher.group(1)));
            }

            Matcher heightMatcher = heightPattern.matcher(videoSection);
            if (heightMatcher.find()) {
                metadata.setHeight(Integer.parseInt(heightMatcher.group(1)));
            }

            Matcher framerateMatcher = frameratePattern.matcher(videoSection);
            if (framerateMatcher.find()) {
                double num = Double.parseDouble(framerateMatcher.group(1));
                double den = Double.parseDouble(framerateMatcher.group(2));
                if (den > 0) {
                    metadata.setFrameRate(num / den);
                }
            }
        }

        // Audio stream
        int audioStreamIndex = json.indexOf("\"codec_type\":\"audio\"");
        if (audioStreamIndex >= 0) {
            String audioSection = json.substring(audioStreamIndex,
                Math.min(audioStreamIndex + 1000, json.length()));
            Matcher audioCodecMatcher = videoCodecPattern.matcher(audioSection);
            if (audioCodecMatcher.find()) {
                metadata.setAudioCodec(audioCodecMatcher.group(1));
            }
        }

        log.debug("Parsed metadata: {}", metadata);
        return metadata;
    }

    /**
     * Check if ffprobe is available.
     */
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffprobePath, "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Exception thrown when ffprobe operations fail.
     */
    public static class FfprobeException extends RuntimeException {
        public FfprobeException(String message) {
            super(message);
        }

        public FfprobeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
