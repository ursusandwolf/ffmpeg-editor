package com.videoeditor.infrastructure.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Runner for executing FFmpeg commands with progress tracking and cancellation support.
 */
public class FfmpegJobRunner {
    private static final Logger log = LoggerFactory.getLogger(FfmpegJobRunner.class);
    private final String ffmpegPath;
    private final ExecutorService executor;
    private Process currentProcess;

    public FfmpegJobRunner() {
        this("ffmpeg");
    }

    public FfmpegJobRunner(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * Run FFmpeg command asynchronously with progress callback.
     */
    public CompletableFuture<FfmpegResult> runAsync(List<String> command, 
                                                     Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> run(command, progressCallback), executor);
    }

    /**
     * Run FFmpeg command synchronously with progress callback.
     */
    public FfmpegResult run(List<String> command, Consumer<String> progressCallback) {
        log.info("Running FFmpeg command: {}", String.join(" ", command));
        
        try {
            // Replace 'ffmpeg' with actual path if needed
            List<String> actualCommand = command;
            if (command.get(0).equals("ffmpeg")) {
                actualCommand = new java.util.ArrayList<>(command);
                actualCommand.set(0, ffmpegPath);
            }

            ProcessBuilder pb = new ProcessBuilder(actualCommand);
            pb.redirectErrorStream(true);
            
            synchronized (this) {
                currentProcess = pb.start();
            }

            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errors.append(line).append("\n");
                    
                    // Parse progress from FFmpeg output
                    if (progressCallback != null && line.contains("time=")) {
                        int timeIndex = line.indexOf("time=");
                        if (timeIndex >= 0) {
                            String timeStr = line.substring(timeIndex + 5, 
                                Math.min(timeIndex + 17, line.length()));
                            progressCallback.accept("Processing: " + timeStr);
                        }
                    }
                }
            }

            int exitCode = currentProcess.waitFor();
            
            synchronized (this) {
                currentProcess = null;
            }

            FfmpegResult result = new FfmpegResult(
                exitCode == 0,
                exitCode,
                output.toString(),
                errors.toString()
            );

            if (exitCode != 0) {
                log.error("FFmpeg failed with exit code {}: {}", exitCode, errors);
            } else {
                log.info("FFmpeg completed successfully");
            }

            return result;

        } catch (IOException e) {
            log.error("Failed to execute FFmpeg", e);
            return new FfmpegResult(false, -1, "", "Failed to execute: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("FFmpeg execution interrupted");
            return new FfmpegResult(false, -1, "", "Interrupted");
        }
    }

    /**
     * Cancel the currently running FFmpeg process.
     */
    public void cancel() {
        synchronized (this) {
            if (currentProcess != null) {
                log.info("Cancelling FFmpeg process");
                currentProcess.destroyForcibly();
                currentProcess = null;
            }
        }
    }

    /**
     * Check if FFmpeg is available.
     */
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
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

    /**
     * Get FFmpeg version string.
     */
    public String getVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                while (reader.readLine() != null) {
                    // drain remaining output to avoid pipe deadlock
                }
                process.waitFor();
                return version != null ? version : "Unknown";
            }
        } catch (IOException e) {
            return "Unknown";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Unknown";
        }
    }

    public void shutdown() {
        cancel();
        executor.shutdown();
    }

    /**
     * Result of an FFmpeg execution.
     */
    public static class FfmpegResult {
        private final boolean success;
        private final int exitCode;
        private final String output;
        private final String error;

        public FfmpegResult(boolean success, int exitCode, String output, String error) {
            this.success = success;
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            return "FfmpegResult{success=" + success + ", exitCode=" + exitCode + "}";
        }
    }
}
