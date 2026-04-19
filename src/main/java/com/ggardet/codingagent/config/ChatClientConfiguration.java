package com.ggardet.codingagent.config;

import org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfiguration {
    @Bean
    public ChatClient chatClient(
            final ChatClient.Builder builder,
            final SessionMemoryAdvisor sessionMemoryAdvisor,
            final AutoMemoryToolsAdvisor autoMemoryToolsAdvisor,
            final ToolCallback[] agentTools
    ) {
        final var toolCallAdvisor = ToolCallAdvisor.builder().disableInternalConversationHistory().build();
        return builder
                .defaultToolCallbacks(agentTools)
                .defaultAdvisors(
                        sessionMemoryAdvisor,
                        autoMemoryToolsAdvisor,
                        toolCallAdvisor)
                .build();
    }
}
