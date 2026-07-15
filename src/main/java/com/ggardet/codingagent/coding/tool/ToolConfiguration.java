package com.ggardet.codingagent.coding.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.ggardet.codingagent.agent.event.ToolEventSink;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/// Declares the tool beans exposed to the model — file system, search, shell, web search, skills,
/// and todo tools — and assembles them into the [ToolCallback] array wired into the chat client.
@Configuration
public class ToolConfiguration {
    /// Provides the file read/write tool.
    ///
    /// @return the file system tool
    @Bean
    public FileSystemTools fileSystemTools() {
        return FileSystemTools.builder().build();
    }

    /// Provides the regex content-search tool.
    ///
    /// @return the grep tool
    @Bean
    public GrepTool grepTool() {
        return GrepTool.builder().build();
    }

    /// Provides the filename glob tool.
    ///
    /// @return the glob tool
    @Bean
    public GlobTool globTool() {
        return GlobTool.builder().build();
    }

    /// Provides the shell-command execution tool.
    ///
    /// @return the shell tool
    @Bean
    public ShellTools shellTools() {
        return ShellTools.builder().build();
    }

    /// Provides the Tavily web-search tool.
    ///
    /// @param tavilyApiKey the Tavily API key resolved from the environment
    /// @return the web-search tool
    @Bean
    public TavilyWebSearchTool tavilyWebSearchTool(final @Value("${TAVILY_API_KEY}") String tavilyApiKey) {
        return TavilyWebSearchTool.builder(tavilyApiKey).build();
    }

    // ponytail: add an entry here when shipping a new default skill in resources/.agent/skills
    private static final List<String> BUNDLED_SKILLS = List.of(
            "git", "tdd", "fd", "grep", "ls", "cat", "mv", "cp", "rm");

    /// Loads skills from an on-disk directory rather than scanning the classpath. Classpath
    /// wildcard scanning (`classpath*:.../**/SKILL.md`) returns nothing in a GraalVM native
    /// image, which makes `SkillsTool.build()` fail its non-empty assertion. Bundled skills
    /// are seeded to the directory on first run.
    @Bean
    public ToolCallback skillsTool() {
        final var skillsDirectory = Path.of(System.getProperty("user.home"), ".agent", "skills");
        seedBundledSkills(skillsDirectory);
        return SkillsTool.builder()
                .addSkillsDirectory(skillsDirectory.toString())
                .build();
    }

    /// Copies each bundled skill's `SKILL.md` from the classpath into the on-disk skills directory
    /// when it is not already present, so the tool has skills to load on a first run.
    ///
    /// @param skillsDirectory the root directory skills are seeded into
    /// @throws IllegalStateException if a bundled skill cannot be read or written
    private void seedBundledSkills(final Path skillsDirectory) {
        for (final var skill : BUNDLED_SKILLS) {
            final var target = skillsDirectory.resolve(skill).resolve("SKILL.md");
            if (Files.exists(target)) {
                continue;
            }
            try {
                Files.createDirectories(target.getParent());
                try (final var input = new ClassPathResource(".agent/skills/" + skill + "/SKILL.md").getInputStream()) {
                    Files.copy(input, target);
                }
            } catch (final IOException exception) {
                throw new IllegalStateException("Failed to seed bundled skill: " + skill, exception);
            }
        }
    }

    /// Provides the todo-list tool, forwarding each update to the UI as a formatted event.
    ///
    /// @param toolEventSink the sink todo updates are published to
    /// @return the todo-write tool
    @Bean
    public TodoWriteTool todoWriteTool(final ToolEventSink toolEventSink) {
        return TodoWriteTool.builder()
                .todoEventHandler(todos -> toolEventSink.emit(formatTodos(todos)))
                .build();
    }

    /// Aggregates the individual tools into the array of tool callbacks exposed to the model.
    ///
    /// @param fileSystemTools the file read/write tool
    /// @param grepTool the content-search tool
    /// @param globTool the filename glob tool
    /// @param shellTools the shell-command tool
    /// @param tavilyWebSearchTool the web-search tool
    /// @param skillsTool the skills tool callback
    /// @param todoWriteTool the todo-list tool
    /// @return the combined array of tool callbacks
    @Bean
    public ToolCallback[] agentTools(
            final FileSystemTools fileSystemTools,
            final GrepTool grepTool,
            final GlobTool globTool,
            final ShellTools shellTools,
            final TavilyWebSearchTool tavilyWebSearchTool,
            final ToolCallback skillsTool,
            final TodoWriteTool todoWriteTool
    ) {
        // SmartWebFetchTool is intentionally excluded: its HTML-to-markdown engine (flexmark)
        // fails to initialize in a GraalVM native image (BitFieldSet reads empty enum constants).
        // Web search via Tavily remains. To restore URL fetching, re-add the tool and initialize
        // flexmark at build time (see native-image buildArgs in the README/pom).
        final var baseTools = ToolCallbacks.from(
                fileSystemTools,
                grepTool,
                globTool,
                shellTools,
                tavilyWebSearchTool,
                todoWriteTool
        );
        return Stream.concat(Arrays.stream(baseTools), Stream.of(skillsTool))
                .toArray(ToolCallback[]::new);
    }

    /// Formats a todo list into a multi-line, icon-prefixed string for display in the UI.
    ///
    /// @param todos the current todo list
    /// @return the formatted task-plan text
    private static String formatTodos(final TodoWriteTool.Todos todos) {
        final var sb = new StringBuilder("📋 Task Plan:");
        for (final var todo : todos.todos()) {
            final var icon = switch (todo.status()) {
                case pending -> "\n  ☐";
                case in_progress -> "\n  ⟳";
                case completed -> "\n  ✓";
            };
            sb.append(icon).append(" ").append(todo.content());
        }
        return sb.toString();
    }
}
