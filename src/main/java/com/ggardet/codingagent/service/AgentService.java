package com.ggardet.codingagent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@Service
public class AgentService {
    private final Resource systemPromptResource;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    private final String sessionId = UUID.randomUUID().toString();

    public AgentService(
            final @Value("classpath:/prompts/system.st") Resource systemPromptResource,
            final ChatClient chatClient,
            final ChatMemory chatMemory) {
        this.systemPromptResource = systemPromptResource;
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    public Flux<String> streamChat(final String message) {
        return chatClient.prompt(message)
                .system(buildSystemPrompt())
                .advisors(advisorSpec -> advisorSpec.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId))
                .toolContext(Map.of("workingDir", System.getProperty("user.dir")))
                .stream()
                .content();
    }

    public void clearMemory() {
        chatMemory.clear(sessionId);
    }

    private String buildSystemPrompt() {
        return new PromptTemplate(systemPromptResource)
                .render(Map.of("workingDir", System.getProperty("user.dir")));
    }
}
