package com.ggardet.codingagent.config;

import java.util.Arrays;
import java.util.stream.Stream;

import org.springaicommunity.agent.tools.BraveWebSearchTool;
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
public class ToolsConfiguration {
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
    public BraveWebSearchTool braveWebSearchTool(final @Value("${BRAVE_API_KEY}") String braveApiKey) {
        return BraveWebSearchTool.builder(braveApiKey).build();
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
            final BraveWebSearchTool braveWebSearchTool,
            final SmartWebFetchTool smartWebFetchTool,
            final ToolCallback skillsTool
    ) {
        final var baseTools = ToolCallbacks.from(
                fileSystemTools,
                grepTool,
                globTool,
                shellTools,
                braveWebSearchTool,
                smartWebFetchTool
        );
        return Stream.concat(Arrays.stream(baseTools), Stream.of(skillsTool))
                .toArray(ToolCallback[]::new);
    }
}
