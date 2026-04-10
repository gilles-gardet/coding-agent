package com.ggardet.codingagent.config;

import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;

@Configuration
public class AgentConfiguration {
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(50).build();
    }

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
    public ChatClient chatClient(
            final ChatClient.Builder builder,
            final ChatMemory chatMemory,
            final FileSystemTools fileSystemTools,
            final GrepTool grepTool,
            final GlobTool globTool,
            final ShellTools shellTools) {
        return builder
                .defaultTools(fileSystemTools, grepTool, globTool, shellTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).scheduler(Schedulers.boundedElastic()).build()
                )
                .build();
    }
}
