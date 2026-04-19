package com.ggardet.codingagent.config;

import java.util.Arrays;
import java.util.stream.Stream;

import com.ggardet.codingagent.tools.TavilyWebSearchTool;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.SmartWebFetchTool;
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
    public SmartWebFetchTool smartWebFetchTool(final ChatClient.Builder chatClientBuilder) {
        return SmartWebFetchTool.builder(chatClientBuilder.build()).build();
    }

    @Bean
    public TavilyWebSearchTool tavilyWebSearchTool(final @Value("${TAVILY_API_KEY}") String tavilyApiKey) {
        return TavilyWebSearchTool.builder(tavilyApiKey).build();
    }

    @Bean
    public ToolCallback skillsTool() {
        return SkillsTool.builder()
                .addSkillsResource(new ClassPathResource(".agent/skills"))
                .build();
    }

    @Bean
    public ToolCallback[] agentTools(
            final FileSystemTools fileSystemTools,
            final GrepTool grepTool,
            final GlobTool globTool,
            final ShellTools shellTools,
            final TavilyWebSearchTool tavilyWebSearchTool,
            final SmartWebFetchTool smartWebFetchTool,
            final ToolCallback skillsTool
    ) {
        final var baseTools = ToolCallbacks.from(
                fileSystemTools,
                grepTool,
                globTool,
                shellTools,
                tavilyWebSearchTool,
                smartWebFetchTool
        );
        return Stream.concat(Arrays.stream(baseTools), Stream.of(skillsTool))
                .toArray(ToolCallback[]::new);
    }
}
