package com.videoeditor.presentation.tui;

import com.videoeditor.domain.Clip;
import com.videoeditor.domain.Project;
import com.videoeditor.domain.Track;

import java.time.Duration;

/**
 * Timeline-derived calculations used by UI and editing actions.
 */
final class TimelineMetrics {
    private TimelineMetrics() {
    }

    static Duration totalDuration(Project project) {
        Duration total = Duration.ZERO;
        for (Track track : project.getTimeline().getTracks()) {
            for (Clip clip : track.getClips()) {
                Duration clipEnd = clip.getTimelinePosition().plus(clip.getDuration());
                if (clipEnd.compareTo(total) > 0) {
                    total = clipEnd;
                }
            }
        }
        return total;
    }
}
