package com.ggardet.codingagent.coding.history;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/// Persistent, navigable history of submitted user inputs. Entries are loaded from and appended to
/// a file under the user's home directory, and can be browsed with up/down navigation that
/// preserves the in-progress draft.
@Component
public class InputHistory {
    private static final Path HISTORY_FILE = Paths.get(System.getProperty("user.home"), ".coding-agent", "history");
    private final List<String> entries = new ArrayList<>();
    private int navigationIndex = -1;
    private String savedDraft = "";

    /// Creates the history and loads any previously persisted entries.
    public InputHistory() {
        loadFromFile();
    }

    /// Adds a message to the history and persists it, unless it is blank or identical to the most
    /// recent entry. Resets navigation to the newest position.
    ///
    /// @param message the submitted input to record
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

    /// Navigates to the previous (older) history entry. Saves the current draft on first navigation
    /// so it can be restored later.
    ///
    /// @param currentDraft the current input text, saved on the first navigation step
    /// @return the older history entry, or the draft when there is no history
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

    /// Navigates to the next (more recent) history entry, or restores the saved draft once past the
    /// newest entry.
    ///
    /// @return the newer history entry, the restored draft, or an empty string when not navigating
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

    /// Resets navigation state and discards any saved draft.
    public void resetNavigation() {
        navigationIndex = -1;
        savedDraft = "";
    }

    /// Loads previously persisted history entries from the history file, ignoring blank lines and
    /// silently tolerating read errors.
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

    /// Appends a single entry to the history file, creating the parent directory if needed and
    /// silently tolerating write errors.
    ///
    /// @param message the entry to append
    private void appendToFile(final String message) {
        try {
            Files.createDirectories(HISTORY_FILE.getParent());
            Files.writeString(HISTORY_FILE, message + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException ignored) {
        }
    }
}
