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
import org.springaicommunity.agent.tools.SmartWebFetchTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ToolConfiguration {
    @Bean
    public FileSystemTools fileSystemTools() {
        return FileSystemTools.builder().build();
    }

    @Bean
    public GrepTool grepTool() {
        return GrepTool.builder().build();
    }

    @Bean
    public GlobTool globTool() {
        return GlobTool.builder().build();
    }

    @Bean
    public ShellTools shellTools() {
        return ShellTools.builder().build();
    }

    @Bean
    public TavilyWebSearchTool tavilyWebSearchTool(final @Value("${TAVILY_API_KEY}") String tavilyApiKey) {
        return TavilyWebSearchTool.builder(tavilyApiKey).build();
    }

    // ponytail: add an entry here when shipping a new default skill in resources/.agent/skills
    private static final List<String> BUNDLED_SKILLS = List.of(
            "git", "tdd", "fd", "grep", "ls", "cat", "mv", "cp", "rm");

    /**
     * Loads skills from an on-disk directory rather than scanning the classpath. Classpath
     * wildcard scanning ({@code classpath*:.../**}{@code /SKILL.md}) returns nothing in a
     * GraalVM native image, which makes {@code SkillsTool.build()} fail its non-empty
     * assertion. Bundled skills are seeded to the directory on first run.
     */
    @Bean
    public ToolCallback skillsTool() {
        final var skillsDirectory = Path.of(System.getProperty("user.home"), ".agent", "skills");
        seedBundledSkills(skillsDirectory);
        return SkillsTool.builder()
                .addSkillsDirectory(skillsDirectory.toString())
                .build();
    }

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

    @Bean
    public TodoWriteTool todoWriteTool(final ToolEventSink toolEventSink) {
        return TodoWriteTool.builder()
                .todoEventHandler(todos -> toolEventSink.emit(formatTodos(todos)))
                .build();
    }

    @Bean
    public ToolCallback[] agentTools(
            final FileSystemTools fileSystemTools,
            final GrepTool grepTool,
            final GlobTool globTool,
            final ShellTools shellTools,
            final TavilyWebSearchTool tavilyWebSearchTool,
            final ChatClient.Builder chatClientBuilder,
            final ToolCallback skillsTool,
            final TodoWriteTool todoWriteTool
    ) {
        final var smartWebFetchTool = SmartWebFetchTool.builder(chatClientBuilder.build()).build();
        final var baseTools = ToolCallbacks.from(
                fileSystemTools,
                grepTool,
                globTool,
                shellTools,
                tavilyWebSearchTool,
                smartWebFetchTool,
                todoWriteTool
        );
        return Stream.concat(Arrays.stream(baseTools), Stream.of(skillsTool))
                .toArray(ToolCallback[]::new);
    }

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
