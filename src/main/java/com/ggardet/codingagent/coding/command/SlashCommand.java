package com.ggardet.codingagent.coding.command;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Slash commands typed in the input as `/name [args]`. Prompt-backed commands expand
/// into an instruction sent to the agent; local commands (with a `null` template) are
/// handled directly by the UI.
public enum SlashCommand {
    REVIEW("review", "Review current code changes for bugs and improvements", """
            Review the current code changes. First run `git status` and `git diff` to see what changed, \
            then review the diff for correctness bugs, edge cases, and simplification opportunities. \
            Report concise findings, most important first.{args}"""),
    ANALYZE("analyze", "Analyze the codebase architecture and quality", """
            Analyze this codebase. Explore the project structure and summarize its architecture, main \
            components, notable patterns, and any technical debt or risks you find.{args}"""),
    EXPLAIN("explain", "Explain a file, function, or concept", """
            Explain the following, reading any relevant files first: {args}"""),
    TEST("test", "Run the test suite and fix failures", """
            Run the project's test suite using the shell tool. If any tests fail, analyze and fix them, \
            then re-run until green. Report what you changed.{args}"""),
    COMMIT("commit", "Stage changes and create a git commit", """
            Create a git commit for the current changes: run git status and git diff, stage the \
            appropriate files, write a clear conventional-commit message, and commit. Do not push.{args}"""),
    PLAN("plan", "Toggle plan mode (plan without executing)", null),
    CLEAR("clear", "Clear the conversation", null),
    HELP("help", "List available commands", null);

    private final String commandName;
    private final String description;
    private final String promptTemplate;

    /// Defines a slash command.
    ///
    /// @param commandName the command name typed after the leading slash
    /// @param description the one-line description shown in autocompletion and help
    /// @param promptTemplate the prompt template with a `{args}` placeholder, or `null` for a
    ///        command handled locally by the UI
    SlashCommand(final String commandName, final String description, final String promptTemplate) {
        this.commandName = commandName;
        this.description = description;
        this.promptTemplate = promptTemplate;
    }

    /// Returns the command name (without the leading slash).
    ///
    /// @return the command name
    public String commandName() {
        return commandName;
    }

    /// Returns the one-line command description.
    ///
    /// @return the description
    public String description() {
        return description;
    }

    /// Expands this command's template with the user-supplied arguments. For [#EXPLAIN]
    /// the arguments are the subject to explain; for the other prompt commands they become an
    /// optional additional-focus suffix.
    ///
    /// @param arguments the raw arguments typed after the command, may be `null` or blank
    /// @return the fully expanded prompt to send to the agent
    public String expand(final String arguments) {
        final var trimmed = Objects.requireNonNullElse(arguments, "").trim();
        final var slot = switch (this) {
            case EXPLAIN -> trimmed;
            default -> trimmed.isEmpty() ? "" : " Additional focus: " + trimmed;
        };
        return promptTemplate.replace("{args}", slot);
    }

    /// Finds the command whose name exactly matches the given value.
    ///
    /// @param name the command name to look up
    /// @return the matching command, or empty if none matches
    public static Optional<SlashCommand> byName(final String name) {
        return Arrays.stream(values()).filter(command -> command.commandName.equals(name)).findFirst();
    }

    /// Returns the commands whose names start with the given prefix, in declaration order.
    ///
    /// @param prefix the prefix to match against command names
    /// @return the matching commands (empty if none)
    public static List<SlashCommand> matching(final String prefix) {
        return Arrays.stream(values()).filter(command -> command.commandName.startsWith(prefix)).toList();
    }
}
