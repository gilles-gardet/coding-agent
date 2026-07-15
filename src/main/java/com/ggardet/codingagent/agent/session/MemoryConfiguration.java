package com.ggardet.codingagent.agent.session;

import org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.RecursiveSummarizationCompactionStrategy;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/// Wires the memory stack: an in-memory session repository for within-session conversation
/// history, a recursive-summarization compaction strategy, the session-memory advisor, and the
/// cross-session auto-memory tools advisor backed by files under the user's home directory.
@Configuration
public class MemoryConfiguration {
    private static final String DEFAULT_USER_ID = "gilles";

    /// Provides the session service backed by an in-memory repository.
    ///
    /// @return the session service used to store per-session conversation memory
    @Bean
    public SessionService sessionService() {
        final var inMemorySessionRepository = InMemorySessionRepository.builder().build();
        return DefaultSessionService.builder().sessionRepository(inMemorySessionRepository).build();
    }

    /// Provides the compaction strategy that summarizes older conversation events to keep the
    /// context bounded.
    ///
    /// @param chatClientBuilder the builder used to create the summarization chat client
    /// @return the compaction strategy retaining the most recent events
    @Bean
    public CompactionStrategy compactionStrategy(final ChatClient.Builder chatClientBuilder) {
        final var chatClient = chatClientBuilder.build();
        return RecursiveSummarizationCompactionStrategy.builder(chatClient).maxEventsToKeep(20).build();
    }

    /// Provides the advisor that persists and restores session memory and triggers compaction once
    /// the turn count threshold is reached.
    ///
    /// @param sessionService the session store
    /// @param compactionStrategy the strategy applied when compaction is triggered
    /// @return the configured session-memory advisor
    @Bean
    public SessionMemoryAdvisor sessionMemoryAdvisor(
            final SessionService sessionService,
            final CompactionStrategy compactionStrategy
    ) {
        final var turnCountTrigger = new TurnCountTrigger(20);
        return SessionMemoryAdvisor.builder(sessionService)
                .defaultUserId(DEFAULT_USER_ID)
                .compactionTrigger(turnCountTrigger)
                .compactionStrategy(compactionStrategy)
                .build();
    }

    /// Provides the advisor exposing cross-session memory tools, backed by files under
    /// `~/.agent/memories`, with occasional automatic memory consolidation.
    ///
    /// @return the configured auto-memory tools advisor
    @Bean
    public AutoMemoryToolsAdvisor autoMemoryToolsAdvisor() {
        final var memoriesRootDirectory = Path.of(System.getProperty("user.home"), ".agent", "memories").toString();
        return AutoMemoryToolsAdvisor.builder()
                .memoriesRootDirectory(memoriesRootDirectory)
                .memoryConsolidationTrigger((_, _) -> Math.random() < 0.05)
                .build();
    }
}
