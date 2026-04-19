package com.ggardet.codingagent.config;

import java.util.Arrays;
import java.util.stream.Stream;

import com.ggardet.codingagent.logging.ToolEventSink;
import com.ggardet.codingagent.tools.TavilyWebSearchTool;
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

    @Bean
    public ToolCallback skillsTool() {
        return SkillsTool.builder()
                .addSkillsResource(new ClassPathResource(".agent/skills"))
                .addSkillsResource(new ClassPathResource("META-INF/skills"))
                .build();
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
