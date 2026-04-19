package com.ggardet.codingagent.config;

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

@Configuration
public class MemoryConfiguration {
    private static final String MEMORY_DIR_MAC = "/Users/gilles/.agent/memories";
    private static final String MEMORY_DIR_LINUX = "/home/gilles/.agent/memories";
    private static final String OS_NAME_LINUX = "Linux";
    private static final String OS_NAME = "os.name";
    private static final String DEFAULT_USER_ID = "gilles";

    @Bean
    public SessionService sessionService() {
        final var inMemorySessionRepository = InMemorySessionRepository.builder().build();
        return DefaultSessionService.builder().sessionRepository(inMemorySessionRepository).build();
    }

    @Bean
    public CompactionStrategy compactionStrategy(final ChatClient.Builder chatClientBuilder) {
        final var chatClient = chatClientBuilder.build();
        return RecursiveSummarizationCompactionStrategy.builder(chatClient).maxEventsToKeep(20).build();
    }

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

    @Bean
    public AutoMemoryToolsAdvisor autoMemoryToolsAdvisor() {
        final var osName = System.getProperty(OS_NAME);
        final var memoriesRootDirectory = osName.contains(OS_NAME_LINUX) ?
                MEMORY_DIR_LINUX :
                MEMORY_DIR_MAC;
        return AutoMemoryToolsAdvisor.builder()
                .memoriesRootDirectory(memoriesRootDirectory)
                .memoryConsolidationTrigger((_, _) -> Math.random() < 0.05)
                .build();
    }
}
