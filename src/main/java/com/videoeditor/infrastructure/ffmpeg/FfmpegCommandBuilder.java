package com.videoeditor.infrastructure.ffmpeg;

import com.videoeditor.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for constructing FFmpeg commands based on project and export settings.
 */
public class FfmpegCommandBuilder {
    private static final Logger log = LoggerFactory.getLogger(FfmpegCommandBuilder.class);

    /**
     * Build FFmpeg command for exporting a project.
     */
    public List<String> buildExportCommand(Project project, ExportSettings settings) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y"); // Overwrite output

        // Add preset arguments
        String[] presetArgs = settings.getPreset().getFfmpegArgs().split(" ");
        for (String arg : presetArgs) {
            if (!arg.isEmpty()) {
                command.add(arg);
            }
        }

        // Build input files from timeline clips
        Timeline timeline = project.getTimeline();
        List<String> inputFiles = new ArrayList<>();
        
        for (Track track : timeline.getTracks()) {
            for (Clip clip : track.getClips()) {
                MediaAsset asset = project.getMediaAssetById(clip.getAssetId());
                if (asset != null && !inputFiles.contains(asset.getPath())) {
                    inputFiles.add(asset.getPath());
                }
            }
        }

        // Add inputs
        for (String inputFile : inputFiles) {
            command.add("-i");
            command.add(inputFile);
        }

        // Build filter complex for timeline composition
        StringBuilder filterComplex = new StringBuilder();
        int inputIndex = 0;
        
        // Simple concatenation for MVP - in full version would handle complex timelines
        List<String> concatInputs = new ArrayList<>();
        for (Track track : timeline.getTracks()) {
            if (track.getType() == TrackType.VIDEO && track.getClipCount() > 0) {
                for (Clip clip : track.getClips()) {
                    MediaAsset asset = project.getMediaAssetById(clip.getAssetId());
                    if (asset != null) {
                        int streamIndex = findStreamIndex(inputFiles, asset.getPath());
                        concatInputs.add("[" + streamIndex + ":v]trim=start=" + 
                            formatDuration(clip.getStartTime()) + 
                            ":end=" + formatDuration(clip.getEndTime()) + 
                            "[v" + inputIndex + "];");
                        
                        concatInputs.add("[" + streamIndex + ":a]atrim=start=" + 
                            formatDuration(clip.getStartTime()) + 
                            ":end=" + formatDuration(clip.getEndTime()) + 
                            "[a" + inputIndex + "];");
                        inputIndex++;
                    }
                }
            }
        }

        // Concatenate all clips
        if (inputIndex > 0) {
            StringBuilder concatVideo = new StringBuilder();
            StringBuilder concatAudio = new StringBuilder();
            
            for (int i = 0; i < inputIndex; i++) {
                concatVideo.append("[v").append(i).append("]");
                concatAudio.append("[a").append(i).append("]");
            }
            
            filterComplex.append(String.join("", concatInputs));
            filterComplex.append(concatVideo).append("concat=n=").append(inputIndex)
                .append(":v=1:a=0[outv];");
            filterComplex.append(concatAudio).append("concat=n=").append(inputIndex)
                .append(":v=0:a=1[outa]");
            
            command.add("-filter_complex");
            command.add(filterComplex.toString());
            command.add("-map");
            command.add("[outv]");
            command.add("-map");
            command.add("[outa]");
        }

        // Video codec settings
        command.add("-c:v");
        command.add(settings.getFormat().getVideoCodec());
        
        // Audio codec settings
        command.add("-c:a");
        command.add(settings.getFormat().getAudioCodec());

        // Bitrate
        command.add("-b:v");
        command.add(settings.getBitrate() + "");

        // Output file
        command.add(settings.getOutputPath());

        log.info("Built FFmpeg command: {}", String.join(" ", command));
        return command;
    }

    /**
     * Build FFmpeg command for extracting metadata (simplified probe alternative).
     */
    public List<String> buildProbeCommand(String filePath) {
        List<String> command = new ArrayList<>();
        command.add("ffprobe");
        command.add("-v");
        command.add("quiet");
        command.add("-print_format");
        command.add("json");
        command.add("-show_format");
        command.add("-show_streams");
        command.add(filePath);
        return command;
    }

    /**
     * Build FFmpeg command for trimming a video.
     */
    public List<String> buildTrimCommand(String inputPath, String outputPath, 
                                          Duration start, Duration end) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(inputPath);
        command.add("-ss");
        command.add(formatDuration(start));
        command.add("-to");
        command.add(formatDuration(end));
        command.add("-c");
        command.add("copy");
        command.add(outputPath);
        return command;
    }

    /**
     * Build FFmpeg command for applying effects to a clip.
     */
    public List<String> buildEffectCommand(String inputPath, String outputPath, 
                                            List<Effect> effects) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(inputPath);

        StringBuilder videoFilters = new StringBuilder();
        StringBuilder audioFilters = new StringBuilder();

        for (Effect effect : effects) {
            switch (effect.getType()) {
                case FADE_IN:
                    Object durationObj = effect.getParameter("duration");
                    double fadeDuration = durationObj != null ?
                        Double.parseDouble(durationObj.toString()) : 1.0;
                    videoFilters.append("fade=t=in:st=0:d=").append(fadeDuration).append(",");
                    break;

                case FADE_OUT:
                    Object outDurationObj = effect.getParameter("duration");
                    double fadeOutDuration = outDurationObj != null ?
                        Double.parseDouble(outDurationObj.toString()) : 1.0;
                    videoFilters.append("fade=t=out:st=0:d=").append(fadeOutDuration).append(",");
                    break;

                case MUTE:
                    audioFilters.append("volume=0,");
                    break;

                case WATERMARK:
                    Object textObj = effect.getParameter("text");
                    if (textObj != null) {
                        String escapedText = textObj.toString()
                            .replace("\\", "\\\\")
                            .replace(":", "\\:")
                            .replace("'", "\\'");
                        videoFilters.append("drawtext=text='").append(escapedText)
                            .append("':fontcolor=white:fontsize=24:x=10:y=10,");
                    }
                    break;

                case SPEED_CHANGE:
                    Object speedObj = effect.getParameter("speed");
                    if (speedObj != null) {
                        double speed = Double.parseDouble(speedObj.toString());
                        if (speed > 0) {
                            videoFilters.append("setpts=").append(1.0 / speed).append("*PTS,");
                            audioFilters.append("atempo=").append(speed).append(",");
                        }
                    }
                    break;

                default:
                    break;
            }
        }

        String videoFilter = trimTrailingComma(videoFilters.toString());
        String audioFilter = trimTrailingComma(audioFilters.toString());

        if (!videoFilter.isEmpty()) {
            command.add("-vf");
            command.add(videoFilter);
        }
        if (!audioFilter.isEmpty()) {
            command.add("-af");
            command.add(audioFilter);
        } else {
            command.add("-c:a");
            command.add("copy");
        }

        command.add(outputPath);

        return command;
    }

    private int findStreamIndex(List<String> files, String path) {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).equals(path)) {
                return i;
            }
        }
        return 0;
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        int nanos = duration.getNano();
        return String.format("%d.%09d", seconds, nanos);
    }

    private static String trimTrailingComma(String filter) {
        if (filter.endsWith(",")) {
            return filter.substring(0, filter.length() - 1);
        }
        return filter;
    }

    /**
     * Escape argument for shell safety (though we use ProcessBuilder).
     */
    public static String escapeArg(String arg) {
        if (arg.contains(" ") || arg.contains("\"") || arg.contains("'")) {
            return "\"" + arg.replace("\"", "\\\"") + "\"";
        }
        return arg;
    }
}
