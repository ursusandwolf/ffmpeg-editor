package com.videoeditor.infrastructure;

import com.videoeditor.domain.*;
import com.videoeditor.infrastructure.ffmpeg.FfmpegCommandBuilder;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FFmpeg command builder.
 */
public class FfmpegCommandBuilderTest {

    private final FfmpegCommandBuilder builder = new FfmpegCommandBuilder();

    @Test
    public void testBuildProbeCommand() {
        List<String> command = builder.buildProbeCommand("/path/to/video.mp4");
        
        assertEquals(8, command.size());
        assertEquals("ffprobe", command.get(0));
        assertEquals("-show_streams", command.get(command.size() - 2));
        assertTrue(command.contains("/path/to/video.mp4"));
    }

    @Test
    public void testBuildTrimCommand() {
        List<String> command = builder.buildTrimCommand(
            "/input.mp4", 
            "/output.mp4",
            Duration.ofSeconds(10),
            Duration.ofSeconds(30)
        );
        
        assertTrue(command.contains("ffmpeg"));
        assertTrue(command.contains("-i"));
        assertTrue(command.contains("/input.mp4"));
        assertTrue(command.contains("-ss"));
        assertTrue(command.contains("-to"));
        assertTrue(command.contains("/output.mp4"));
        assertTrue(command.contains("copy"));
    }

    @Test
    public void testBuildExportCommandWithProject() {
        // Create test project
        Project project = new Project();
        project.setName("Test Project");
        
        // Add media asset
        MediaAsset asset = new MediaAsset("/test/video.mp4", Duration.ofSeconds(60));
        project.addMediaAsset(asset);
        
        // Add clip to timeline
        Clip clip = new Clip(
            asset.getId(),
            Duration.ZERO,
            Duration.ofSeconds(30),
            Duration.ZERO
        );
        project.getTimeline().getVideoTrack().addClip(clip);
        
        // Configure export settings
        ExportSettings settings = new ExportSettings();
        settings.setOutputPath("/output/result.mp4");
        settings.setFormat(ExportFormat.MP4);
        settings.setPreset(ExportPreset.FAST);
        settings.setBitrate(3000000);
        
        List<String> command = builder.buildExportCommand(project, settings);
        
        assertTrue(command.contains("ffmpeg"));
        assertTrue(command.contains("-y"));
        assertTrue(command.contains("-preset"));
        assertTrue(command.contains("fast"));
        assertTrue(command.contains("/output/result.mp4"));
    }

    @Test
    public void testBuildEffectCommandWithFadeIn() {
        Effect fadeIn = new Effect(Effect.EffectType.FADE_IN);
        fadeIn.setParameter("duration", 2.0);
        
        List<String> command = builder.buildEffectCommand(
            "/input.mp4",
            "/output.mp4",
            List.of(fadeIn)
        );
        
        assertTrue(command.contains("ffmpeg"));
        assertTrue(command.contains("-vf"));
        assertTrue(command.contains("/input.mp4"));
        assertTrue(command.contains("/output.mp4"));
    }

    @Test
    public void testBuildEffectCommandWithWatermark() {
        Effect watermark = new Effect(Effect.EffectType.WATERMARK);
        watermark.setParameter("text", "My Video");
        
        List<String> command = builder.buildEffectCommand(
            "/input.mp4",
            "/output.mp4",
            List.of(watermark)
        );
        
        assertTrue(command.contains("-vf"));
        String filterArg = command.get(command.indexOf("-vf") + 1);
        assertTrue(filterArg.contains("drawtext"));
        assertTrue(filterArg.contains("My Video"));
    }

    @Test
    public void testBuildEffectCommandWithMute() {
        Effect mute = new Effect(Effect.EffectType.MUTE);
        
        List<String> command = builder.buildEffectCommand(
            "/input.mp4",
            "/output.mp4",
            List.of(mute)
        );
        
        assertTrue(command.contains("-af"));
        String filterArg = command.get(command.indexOf("-af") + 1);
        assertTrue(filterArg.contains("volume=0"));
    }

    @Test
    public void testEscapeArg() {
        assertEquals("simple", FfmpegCommandBuilder.escapeArg("simple"));
        assertTrue(FfmpegCommandBuilder.escapeArg("with space").contains("\""));
        assertTrue(FfmpegCommandBuilder.escapeArg("with\"quote").contains("\\\""));
    }

    @Test
    public void testExportFormatCodecs() {
        Project project = new Project();
        ExportSettings mp4Settings = new ExportSettings();
        mp4Settings.setFormat(ExportFormat.MP4);
        mp4Settings.setOutputPath("/out.mp4");
        
        List<String> mp4Command = builder.buildExportCommand(project, mp4Settings);
        assertTrue(mp4Command.contains("libx264"));
        assertTrue(mp4Command.contains("aac"));
        
        ExportSettings webmSettings = new ExportSettings();
        webmSettings.setFormat(ExportFormat.WEBM);
        webmSettings.setOutputPath("/out.webm");
        
        List<String> webmCommand = builder.buildExportCommand(project, webmSettings);
        assertTrue(webmCommand.contains("libvpx-vp9"));
    }
}
