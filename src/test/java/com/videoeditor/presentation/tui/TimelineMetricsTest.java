package com.videoeditor.presentation.tui;

import com.videoeditor.domain.Clip;
import com.videoeditor.domain.EntityId;
import com.videoeditor.domain.Project;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimelineMetricsTest {

    @Test
    void totalDurationUsesLatestClipEnd() {
        Project project = new Project();
        project.getTimeline().getVideoTrack().addClip(new Clip(
                EntityId.generate(),
                Duration.ZERO,
                Duration.ofSeconds(10),
                Duration.ZERO
        ));
        project.getTimeline().getVideoTrack().addClip(new Clip(
                EntityId.generate(),
                Duration.ZERO,
                Duration.ofSeconds(5),
                Duration.ofSeconds(20)
        ));

        assertEquals(Duration.ofSeconds(25), TimelineMetrics.totalDuration(project));
    }
}
