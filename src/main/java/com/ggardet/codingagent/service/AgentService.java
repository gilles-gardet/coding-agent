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

@Service
public class AgentService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final Resource systemPromptResource;

    public static final String CONVERSATION_ID = "default";

    public AgentService(
            final ChatClient chatClient,
            final ChatMemory chatMemory,
            final @Value("classpath:/prompts/system.st") Resource systemPromptResource) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.systemPromptResource = systemPromptResource;
    }

    public Flux<String> streamChat(final String message) {
        return chatClient.prompt(message)
                .system(buildSystemPrompt())
                .advisors(advisorSpec -> advisorSpec.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "session-abc"))
                .toolContext(Map.of("workingDir", System.getProperty("user.dir")))
                .stream()
                .content();
    }

    public void clearMemory() {
        chatMemory.clear(CONVERSATION_ID);
    }

    private String buildSystemPrompt() {
        return new PromptTemplate(systemPromptResource)
                .render(Map.of("workingDir", System.getProperty("user.dir")));
    }
}
