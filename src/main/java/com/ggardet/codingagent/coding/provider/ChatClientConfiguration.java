package com.ggardet.codingagent.coding.provider;

import com.ggardet.codingagent.agent.loop.MaxIterationsToolCallAdvisor;
import org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor;
import org.springframework.ai.chat.client.ChatClient;
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
            final MaxIterationsToolCallAdvisor maxIterationsToolCallAdvisor,
            final ToolCallback[] agentTools
    ) {
        return builder
                .defaultToolCallbacks(agentTools)
                .defaultAdvisors(
                        sessionMemoryAdvisor,
                        autoMemoryToolsAdvisor,
                        maxIterationsToolCallAdvisor)
                .build();
    }
}
