package com.videoeditor.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.videoeditor.domain.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Project persistence layer using JSON serialization.
 */
public class ProjectRepository {
    private static final Logger log = LoggerFactory.getLogger(ProjectRepository.class);
    private final ObjectMapper objectMapper;

    public ProjectRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Save project to a JSON file.
     */
    public void save(Project project, Path path) throws IOException {
        log.info("Saving project to: {}", path);
        
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        objectMapper.writeValue(path.toFile(), project);
        log.info("Project saved successfully");
    }

    /**
     * Load project from a JSON file.
     */
    public Project load(Path path) throws IOException {
        log.info("Loading project from: {}", path);
        
        if (!Files.exists(path)) {
            throw new IOException("Project file not found: " + path);
        }

        Project project = objectMapper.readValue(path.toFile(), Project.class);
        
        // Validate schema version
        // Note: In a real implementation, we'd read the raw JSON first to check version
        log.info("Project loaded successfully: {}", project.getName());
        
        return project;
    }

    /**
     * Check if a file is a valid project file.
     */
    public boolean isValidProjectFile(Path path) {
        try {
            if (!Files.exists(path) || !path.toString().endsWith(".json")) {
                return false;
            }
            
            // Try to parse and validate
            String content = Files.readString(path);
            return content.contains("\"schemaVersion\"") || 
                   content.contains("\"@class\"") ||
                   content.contains("\"id\"");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Create a backup of the project file.
     */
    public void backup(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        
        Path backupPath = path.resolveSibling(
            path.getFileName().toString() + ".bak"
        );
        
        Files.copy(path, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        log.info("Created backup: {}", backupPath);
    }

    /**
     * Get list of recent project files in a directory.
     */
    public java.util.List<Path> findRecentProjects(Path directory, int limit) {
        try {
            return Files.walk(directory)
                .filter(p -> p.toString().endsWith(".json"))
                .filter(this::isValidProjectFile)
                .sorted((a, b) -> {
                    try {
                        return Long.compare(
                            Files.getLastModifiedTime(b).toMillis(),
                            Files.getLastModifiedTime(a).toMillis()
                        );
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .limit(limit)
                .toList();
        } catch (IOException e) {
            log.error("Failed to find recent projects", e);
            return java.util.Collections.emptyList();
        }
    }
}
