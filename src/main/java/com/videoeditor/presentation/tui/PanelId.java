package com.videoeditor.presentation.tui;

/**
 * Focusable panels in the editor layout.
 */
enum PanelId {
    MEDIA(0),
    TIMELINE(1),
    INSPECTOR(2);

    private final int index;

    PanelId(int index) {
        this.index = index;
    }

    int index() {
        return index;
    }

    static PanelId fromIndex(int index) {
        for (PanelId panel : values()) {
            if (panel.index == index) {
                return panel;
            }
        }
        return MEDIA;
    }

    PanelId previous() {
        return fromIndex(Math.max(0, index - 1));
    }

    PanelId next() {
        return fromIndex(Math.min(values().length - 1, index + 1));
    }
}
