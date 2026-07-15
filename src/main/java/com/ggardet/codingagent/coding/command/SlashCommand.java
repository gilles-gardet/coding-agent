package com.ggardet.codingagent.coding.command;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Slash commands typed in the input as {@code /name [args]}. Prompt-backed commands expand
 * into an instruction sent to the agent; local commands (with a {@code null} template) are
 * handled directly by the UI.
 */
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

    SlashCommand(final String commandName, final String description, final String promptTemplate) {
        this.commandName = commandName;
        this.description = description;
        this.promptTemplate = promptTemplate;
    }

    public String commandName() {
        return commandName;
    }

    public String description() {
        return description;
    }

    /**
     * Expands this command's template with the user-supplied arguments. For {@link #EXPLAIN}
     * the arguments are the subject to explain; for the other prompt commands they become an
     * optional additional-focus suffix.
     */
    public String expand(final String arguments) {
        final var trimmed = arguments == null ? "" : arguments.trim();
        final var slot = switch (this) {
            case EXPLAIN -> trimmed;
            default -> trimmed.isEmpty() ? "" : " Additional focus: " + trimmed;
        };
        return promptTemplate.replace("{args}", slot);
    }

    public static Optional<SlashCommand> byName(final String name) {
        return Arrays.stream(values()).filter(command -> command.commandName.equals(name)).findFirst();
    }

    public static List<SlashCommand> matching(final String prefix) {
        return Arrays.stream(values()).filter(command -> command.commandName.startsWith(prefix)).toList();
    }
}
