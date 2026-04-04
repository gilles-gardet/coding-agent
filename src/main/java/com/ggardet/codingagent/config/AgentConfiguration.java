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

@Configuration
public class AgentConfiguration {
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(50).build();
    }

    @Bean
    public ChatClient chatClient(final ChatClient.Builder builder, final ChatMemory chatMemory) {
        return builder
                .defaultSystem("""
                        You are a helpful coding assistant. You have access to tools \
                        for reading files, searching code, running shell commands, \
                        and editing files. Use them to help the user with their codebase.
                        """)
                .defaultTools(
                        FileSystemTools.builder().build(),
                        GrepTool.builder().build(),
                        GlobTool.builder().build(),
                        ShellTools.builder().build()
                )
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
