package com.videoeditor.infrastructure.ffmpeg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for interacting with ffprobe to extract media metadata.
 */
public class FfprobeClient {
    private static final Logger log = LoggerFactory.getLogger(FfprobeClient.class);

    private final String ffprobePath;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;

    public FfprobeClient() {
        this("ffprobe");
    }

    public FfprobeClient(String ffprobePath) {
        this.ffprobePath = ffprobePath;
        this.executor = Executors.newCachedThreadPool();
        this.objectMapper = new ObjectMapper();
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

            return parseJsonOutput(output.toString());

        } catch (IOException e) {
            log.error("Failed to execute ffprobe", e);
            throw new FfprobeException("Failed to execute ffprobe: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FfprobeException("ffprobe interrupted", e);
        }
    }

    private MediaMetadata parseJsonOutput(String json) throws IOException {
        MediaMetadata metadata = new MediaMetadata();
        JsonNode root = objectMapper.readTree(json);

        JsonNode format = root.path("format");
        if (format.hasNonNull("duration")) {
            double seconds = format.get("duration").asDouble();
            metadata.setDuration(Duration.ofMillis(Math.round(seconds * 1000)));
        }
        if (format.has("size")) {
            metadata.setFileSize(format.get("size").asLong());
        }

        JsonNode streams = root.path("streams");
        if (streams.isArray()) {
            for (JsonNode stream : streams) {
                String codecType = stream.path("codec_type").asText();
                if ("video".equals(codecType) && metadata.getVideoCodec() == null) {
                    metadata.setVideoCodec(stream.path("codec_name").asText(null));
                    metadata.setWidth(stream.path("width").asInt(0));
                    metadata.setHeight(stream.path("height").asInt(0));
                    metadata.setFrameRate(parseFrameRate(stream.path("r_frame_rate").asText(null)));
                } else if ("audio".equals(codecType) && metadata.getAudioCodec() == null) {
                    metadata.setAudioCodec(stream.path("codec_name").asText(null));
                }
            }
        }

        log.debug("Parsed metadata: {}", metadata);
        return metadata;
    }

    private static double parseFrameRate(String rate) {
        if (rate == null || rate.isBlank() || "0/0".equals(rate)) {
            return 0;
        }
        String[] parts = rate.split("/");
        if (parts.length == 2) {
            double den = Double.parseDouble(parts[1]);
            if (den > 0) {
                return Double.parseDouble(parts[0]) / den;
            }
        }
        return Double.parseDouble(rate);
    }

    /**
     * Check if ffprobe is available.
     */
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffprobePath, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.getInputStream().readAllBytes();
            return process.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
