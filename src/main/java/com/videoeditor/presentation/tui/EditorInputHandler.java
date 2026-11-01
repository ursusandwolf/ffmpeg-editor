package com.videoeditor.presentation.tui;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.videoeditor.domain.Project;

/**
 * Maps keyboard input to editor navigation and actions.
 *
 * @return {@code false} when the application should quit
 */
final class EditorInputHandler {
    private final Project project;
    private final EditorUiState ui;
    private final EditorActions actions;

    EditorInputHandler(Project project, EditorUiState ui, EditorActions actions) {
        this.project = project;
        this.ui = ui;
        this.actions = actions;
    }

    boolean handle(KeyStroke key) {
        if (ui.isExporting()) {
            if (key.getKeyType() == KeyType.Escape) {
                actions.cancelExport();
            }
            return true;
        }

        if (key.isCtrlDown() && key.getKeyType() == KeyType.Character) {
            Character c = key.getCharacter();
            if (c == null) {
                return true;
            }
            return handleCtrl(c);
        }

        if (key.getKeyType() == KeyType.Character) {
            Character ch = key.getCharacter();
            if (ch != null && (ch == 'q' || ch == 'Q')) {
                return false;
            }
        }
        if (key.getKeyType() == KeyType.EOF) {
            return false;
        }

        switch (key.getKeyType()) {
            case ArrowLeft -> ui.setSelectedPanel(ui.getSelectedPanel().previous());
            case ArrowRight -> ui.setSelectedPanel(ui.getSelectedPanel().next());
            case ArrowUp -> handleArrowUp();
            case ArrowDown -> handleArrowDown();
            case Delete -> actions.deleteSelection();
            case Enter -> actions.addSelectedMediaToTimeline();
            case Character -> handleCharacter(key.getCharacter());
            default -> {
            }
        }
        return true;
    }

    private boolean handleCtrl(char c) {
        switch (Character.toLowerCase(c)) {
            case 'o' -> actions.promptImport();
            case 's' -> actions.saveProject();
            case 'e' -> actions.exportProject();
            case 'r' -> actions.refreshMetadata();
            default -> {
            }
        }
        return true;
    }

    private void handleArrowUp() {
        if (ui.getSelectedPanel() == PanelId.MEDIA) {
            ui.moveMediaSelection(-1, project.getMediaAssets().size());
        } else if (ui.getSelectedPanel() == PanelId.TIMELINE) {
            ui.moveClipSelection(-1, project.getTimeline().getVideoTrack().getClipCount());
        }
    }

    private void handleArrowDown() {
        if (ui.getSelectedPanel() == PanelId.MEDIA) {
            ui.moveMediaSelection(1, project.getMediaAssets().size());
        } else if (ui.getSelectedPanel() == PanelId.TIMELINE) {
            ui.moveClipSelection(1, project.getTimeline().getVideoTrack().getClipCount());
        }
    }

    private void handleCharacter(Character c) {
        if (c == null) {
            return;
        }
        if (c == 's' || c == 'S') {
            actions.splitClipAtPlayhead();
        } else if (c == '+' || c == '=') {
            actions.addSelectedMediaToTimeline();
        } else if (c == ' ') {
            actions.resetPlayback();
        }
    }
}
