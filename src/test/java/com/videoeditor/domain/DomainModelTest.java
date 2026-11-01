package com.videoeditor;

import com.videoeditor.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for domain model.
 */
public class DomainModelTest {

    @Test
    public void testEntityIdGeneration() {
        EntityId id1 = EntityId.generate();
        EntityId id2 = EntityId.generate();
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
        assertEquals(id1, id1);
    }

    @Test
    public void testMediaAssetCreation() {
        Duration duration = Duration.ofSeconds(120);
        MediaAsset asset = new MediaAsset("/path/to/video.mp4", duration);
        
        assertNotNull(asset.getId());
        assertEquals("/path/to/video.mp4", asset.getPath());
        assertEquals(duration, asset.getDuration());
    }

    @Test
    public void testClipCreation() {
        EntityId assetId = EntityId.generate();
        Duration start = Duration.ZERO;
        Duration end = Duration.ofSeconds(30);
        Duration position = Duration.ZERO;
        
        Clip clip = new Clip(assetId, start, end, position);
        
        assertNotNull(clip.getId());
        assertEquals(assetId, clip.getAssetId());
        assertEquals(Duration.ofSeconds(30), clip.getDuration());
        assertTrue(clip.getEffects().isEmpty());
    }

    @Test
    public void testClipWithEffects() {
        Clip clip = new Clip(EntityId.generate(), Duration.ZERO, 
                            Duration.ofSeconds(60), Duration.ZERO);
        
        Effect fadeEffect = new Effect(Effect.EffectType.FADE_IN);
        fadeEffect.setParameter("duration", 2.0);
        clip.addEffect(fadeEffect);
        
        assertEquals(1, clip.getEffects().size());
        assertEquals(Effect.EffectType.FADE_IN, clip.getEffects().get(0).getType());
    }

    @Test
    public void testTrackOperations() {
        Track track = new Track(TrackType.VIDEO);
        assertEquals(0, track.getClipCount());
        
        Clip clip1 = new Clip(EntityId.generate(), Duration.ZERO, 
                             Duration.ofSeconds(30), Duration.ZERO);
        Clip clip2 = new Clip(EntityId.generate(), Duration.ZERO, 
                             Duration.ofSeconds(45), Duration.ofSeconds(30));
        
        track.addClip(clip1);
        track.addClip(clip2);
        
        assertEquals(2, track.getClipCount());
        
        track.removeClip(clip1);
        assertEquals(1, track.getClipCount());
    }

    @Test
    public void testTrackInsertClipMutatesBackingList() {
        Track track = new Track(TrackType.VIDEO);
        Clip first = new Clip(EntityId.generate(), Duration.ZERO,
                Duration.ofSeconds(10), Duration.ZERO);
        Clip second = new Clip(EntityId.generate(), Duration.ofSeconds(10),
                Duration.ofSeconds(20), Duration.ofSeconds(10));

        track.addClip(first);
        track.insertClip(1, second);

        assertEquals(2, track.getClipCount());
        assertEquals(second.getId(), track.getClips().get(1).getId());
    }

    @Test
    public void testTimelineDefaultTracks() {
        Timeline timeline = new Timeline();
        
        Track videoTrack = timeline.getVideoTrack();
        Track audioTrack = timeline.getAudioTrack();
        
        assertNotNull(videoTrack);
        assertNotNull(audioTrack);
        assertEquals(TrackType.VIDEO, videoTrack.getType());
        assertEquals(TrackType.AUDIO, audioTrack.getType());
    }

    @Test
    public void testProjectCreation() {
        Project project = new Project();
        
        assertNotNull(project.getId());
        assertEquals("Untitled Project", project.getName());
        assertTrue(project.getMediaAssets().isEmpty());
        assertNotNull(project.getTimeline());
        assertNotNull(project.getSettings());
    }

    @Test
    public void testExportSettings() {
        ExportSettings settings = new ExportSettings();
        
        assertEquals(ExportFormat.MP4, settings.getFormat());
        assertEquals(ExportPreset.BALANCED, settings.getPreset());
        assertEquals(1920, settings.getWidth());
        assertEquals(1080, settings.getHeight());
    }

    @Test
    public void testExportFormatValues() {
        assertEquals("mp4", ExportFormat.MP4.getExtension());
        assertEquals("libx264", ExportFormat.MP4.getVideoCodec());
        assertEquals("aac", ExportFormat.MP4.getAudioCodec());
        
        assertEquals("webm", ExportFormat.WEBM.getExtension());
        assertEquals("libvpx-vp9", ExportFormat.WEBM.getVideoCodec());
    }

    @Test
    public void testExportPresetValues() {
        assertTrue(ExportPreset.FAST.getFfmpegArgs().contains("fast"));
        assertTrue(ExportPreset.HIGH_QUALITY.getFfmpegArgs().contains("slow"));
    }
}
