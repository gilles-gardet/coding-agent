package com.ggardet.codingagent.history;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class InputHistory {
    private static final Path HISTORY_FILE = Paths.get(System.getProperty("user.home"), ".coding-agent", "history");
    private final List<String> entries = new ArrayList<>();
    private int navigationIndex = -1;
    private String savedDraft = "";

    public InputHistory() {
        loadFromFile();
    }

    public void add(final String message) {
        if (message.isBlank()) {
            return;
        }
        if (!entries.isEmpty() && entries.getLast().equals(message)) {
            navigationIndex = -1;
            return;
        }
        entries.add(message);
        navigationIndex = -1;
        appendToFile(message);
    }

    /**
     * Navigates to the previous (older) history entry.
     * Saves the current draft on first navigation so it can be restored.
     */
    public String navigateUp(final String currentDraft) {
        if (entries.isEmpty()) {
            return currentDraft;
        }
        if (navigationIndex == -1) {
            savedDraft = currentDraft;
            navigationIndex = entries.size() - 1;
        } else if (navigationIndex > 0) {
            navigationIndex--;
        }
        return entries.get(navigationIndex);
    }

    /**
     * Navigates to the next (more recent) history entry, or restores the draft.
     */
    public String navigateDown() {
        if (navigationIndex == -1) {
            return "";
        }
        if (navigationIndex < entries.size() - 1) {
            navigationIndex++;
            return entries.get(navigationIndex);
        }
        navigationIndex = -1;
        return savedDraft;
    }

    public void resetNavigation() {
        navigationIndex = -1;
        savedDraft = "";
    }

    private void loadFromFile() {
        if (!Files.exists(HISTORY_FILE)) {
            return;
        }
        try {
            Files.readAllLines(HISTORY_FILE).stream()
                    .filter(line -> !line.isBlank())
                    .forEach(entries::add);
        } catch (final IOException ignored) {
        }
    }

    private void appendToFile(final String message) {
        try {
            Files.createDirectories(HISTORY_FILE.getParent());
            Files.writeString(HISTORY_FILE, message + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException ignored) {
        }
    }
}
