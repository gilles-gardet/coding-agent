package com.ggardet.codingagent.config;

import org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
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
public class AgentConfiguration {
    private static final String MEMORY_DIR_MAC = "/Users/gilles/.agent/memories";
    private static final String MEMORY_DIR_LINUX = "/home/gilles/.agent/memories";
    private static final String OS_NAME_LINUX = "Linux";
    private static final String OS_NAME = "os.name";

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
        final var turnCountTrigger = new TurnCountTrigger(10);
        return SessionMemoryAdvisor.builder(sessionService)
                .defaultUserId("gilles")
                .compactionTrigger(turnCountTrigger)
                .compactionStrategy(compactionStrategy)
                .build();
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
            final SessionMemoryAdvisor sessionMemoryAdvisor,
            final FileSystemTools fileSystemTools,
            final GrepTool grepTool,
            final GlobTool globTool,
            final ShellTools shellTools) {
        final var osName = System.getProperty(OS_NAME);
        final var memoriesRootDirectory = osName.contains(OS_NAME_LINUX) ?
                MEMORY_DIR_LINUX :
                MEMORY_DIR_MAC;
        final var autoMemoryToolsAdvisor = AutoMemoryToolsAdvisor.builder()
                .memoriesRootDirectory(memoriesRootDirectory)
                .memoryConsolidationTrigger((_, _) -> Math.random() < 0.05)
                .build();
        final var toolCallAdvisor = ToolCallAdvisor.builder().disableInternalConversationHistory().build();
        return builder
                .defaultTools(fileSystemTools, grepTool, globTool, shellTools)
                .defaultAdvisors(
                        sessionMemoryAdvisor,
                        autoMemoryToolsAdvisor,
                        toolCallAdvisor)
                .build();
    }
}
