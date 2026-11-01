package com.videoeditor.presentation.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.videoeditor.application.commands.ExportProjectCommand;
import com.videoeditor.application.commands.ImportMediaCommand;
import com.videoeditor.domain.Project;
import com.videoeditor.infrastructure.persistence.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Application entry point: boots dependencies and runs the TUI event loop.
 */
public class VideoEditorApp {
    private static final Logger log = LoggerFactory.getLogger(VideoEditorApp.class);

    private Screen screen;
    private EditorRenderer renderer;
    private EditorInputHandler inputHandler;
    private EditorActions actions;

    public static void main(String[] args) {
        VideoEditorApp app = new VideoEditorApp();
        try {
            app.run(args);
        } catch (Exception e) {
            log.error("Application error", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public void run() throws IOException {
        run(new String[0]);
    }

    public void run(String[] args) throws IOException {
        Project project = new Project();
        ImportMediaCommand importCommand = new ImportMediaCommand();
        ExportProjectCommand exportCommand = new ExportProjectCommand();
        ProjectRepository projectRepository = new ProjectRepository();
        EditorUiState ui = new EditorUiState();

        actions = new EditorActions(project, ui, importCommand, exportCommand, projectRepository);

        if (!exportCommand.isFfmpegAvailable()) {
            System.err.println("Warning: FFmpeg not found. Export functionality will be limited.");
        } else {
            log.info("FFmpeg available: {}", exportCommand.getFfmpegVersion());
        }

        Terminal terminal = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(120, 40))
                .createTerminal();

        screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.setCursorPosition(null);

        ScreenCanvas canvas = new ScreenCanvas(screen);
        renderer = new EditorRenderer(canvas, project, ui);
        inputHandler = new EditorInputHandler(project, ui, actions);

        if (args != null) {
            for (String arg : args) {
                if (arg != null && !arg.isBlank()) {
                    actions.importFile(arg);
                }
            }
        }

        log.info("Video Editor TUI started");

        boolean running = true;
        while (running) {
            renderer.render();

            KeyStroke keyStroke = screen.pollInput();
            if (keyStroke == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            running = inputHandler.handle(keyStroke);
        }

        cleanup();
    }

    /**
     * Imports a media file into the current session (used by tests / CLI).
     */
    public void importFile(String filePath) {
        if (actions != null) {
            actions.importFile(filePath);
        }
    }

    private void cleanup() {
        try {
            if (screen != null) {
                screen.close();
            }
        } catch (IOException e) {
            log.error("Error closing screen", e);
        }
    }
}
