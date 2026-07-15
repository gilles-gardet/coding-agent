package com.ggardet.codingagent.coding.provider;

import com.ggardet.codingagent.agent.loop.MaxIterationsToolCallAdvisor;
import org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Assembles the application's [ChatClient], registering the agent's tools and the default
/// advisors (session memory, auto-memory tools, and the tool-call iteration cap).
@Configuration
public class ChatClientConfiguration {
    /// Builds the chat client with the agent tools and default advisors applied.
    ///
    /// @param builder the auto-configured chat client builder
    /// @param sessionMemoryAdvisor the advisor managing session memory
    /// @param autoMemoryToolsAdvisor the advisor exposing cross-session memory tools
    /// @param maxIterationsToolCallAdvisor the advisor capping tool-call iterations per turn
    /// @param agentTools the tool callbacks made available to the model
    /// @return the fully configured chat client
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
