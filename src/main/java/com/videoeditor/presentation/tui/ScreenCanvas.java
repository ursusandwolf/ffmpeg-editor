package com.videoeditor.presentation.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;

/**
 * Thin wrapper around Lanterna Screen for common drawing operations.
 */
final class ScreenCanvas {
    private final Screen screen;

    ScreenCanvas(Screen screen) {
        this.screen = screen;
    }

    Screen screen() {
        return screen;
    }

    void put(int x, int y, String text) {
        screen.newTextGraphics().putString(x, y, text);
    }

    void put(int x, int y, String text, TextColor color) {
        TextGraphics graphics = screen.newTextGraphics();
        graphics.setForegroundColor(color);
        graphics.putString(x, y, text);
        graphics.setForegroundColor(TextColor.ANSI.DEFAULT);
    }

    void drawBox(int x, int y, int width, int height, String title) {
        put(x, y, "┌");
        put(x + 1, y, "─".repeat(Math.max(0, width - 2)));
        put(x + width - 1, y, "┐");

        if (title != null && !title.isEmpty()) {
            put(x + 2, y, " " + title + " ", TextColor.ANSI.GREEN);
        }

        for (int i = 1; i < height - 1; i++) {
            put(x, y + i, "│");
            put(x + width - 1, y + i, "│");
        }

        put(x, y + height - 1, "└");
        put(x + 1, y + height - 1, "─".repeat(Math.max(0, width - 2)));
        put(x + width - 1, y + height - 1, "┘");
    }
}
