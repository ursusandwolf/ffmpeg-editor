package com.videoeditor.presentation.tui;

import java.time.Duration;

/**
 * Pure text helpers for the terminal UI.
 */
final class TuiText {
    private TuiText() {
    }

    static String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }

    static String tailPath(String path, int maxLen) {
        if (path == null) {
            return "";
        }
        String tail = path.substring(Math.max(0, path.length() - maxLen));
        return truncate(tail, maxLen);
    }
}
