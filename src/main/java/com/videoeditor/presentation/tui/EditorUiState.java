package com.videoeditor.presentation.tui;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mutable UI session state shared by renderer, input handler and actions.
 */
final class EditorUiState {
    private PanelId selectedPanel = PanelId.MEDIA;
    private int selectedMediaIndex;
    private int selectedClipIndex;
    private Duration playheadPosition = Duration.ZERO;
    private final AtomicBoolean exporting = new AtomicBoolean(false);
    private volatile String statusMessage = "Ready";
    private volatile String lastProgress = "";

    PanelId getSelectedPanel() {
        return selectedPanel;
    }

    void setSelectedPanel(PanelId selectedPanel) {
        this.selectedPanel = selectedPanel;
    }

    int getSelectedMediaIndex() {
        return selectedMediaIndex;
    }

    void setSelectedMediaIndex(int selectedMediaIndex) {
        this.selectedMediaIndex = Math.max(0, selectedMediaIndex);
    }

    void moveMediaSelection(int delta, int mediaCount) {
        if (mediaCount <= 0) {
            selectedMediaIndex = 0;
            return;
        }
        selectedMediaIndex = Math.min(mediaCount - 1, Math.max(0, selectedMediaIndex + delta));
    }

    int getSelectedClipIndex() {
        return selectedClipIndex;
    }

    void setSelectedClipIndex(int selectedClipIndex) {
        this.selectedClipIndex = Math.max(0, selectedClipIndex);
    }

    void moveClipSelection(int delta, int clipCount) {
        if (clipCount <= 0) {
            selectedClipIndex = 0;
            return;
        }
        selectedClipIndex = Math.min(clipCount - 1, Math.max(0, selectedClipIndex + delta));
    }

    Duration getPlayheadPosition() {
        return playheadPosition;
    }

    void setPlayheadPosition(Duration playheadPosition) {
        this.playheadPosition = playheadPosition != null ? playheadPosition : Duration.ZERO;
    }

    boolean isExporting() {
        return exporting.get();
    }

    void setExporting(boolean value) {
        exporting.set(value);
    }

    String getStatusMessage() {
        return statusMessage;
    }

    void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage != null ? statusMessage : "";
    }

    String getLastProgress() {
        return lastProgress;
    }

    void setLastProgress(String lastProgress) {
        this.lastProgress = lastProgress != null ? lastProgress : "";
    }
}
