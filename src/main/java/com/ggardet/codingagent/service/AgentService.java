package com.ggardet.codingagent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AgentService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final List<String> history;

    public static final String CONVERSATION_ID = "default";

    public AgentService(final ChatClient chatClient, final ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.history = new ArrayList<>();
    }

    public String chat(final String message) {
        try {
            final var response = chatClient.prompt(message)
                    .toolContext(Map.of("workingDir", System.getProperty("user.dir")))
                    .call()
                    .content();
            if (response == null) {
                return "Error: the model returned an empty response";
            }
            history.add("> " + message);
            history.add("  " + response);
            return response;
        } catch (final Exception exception) {
            return "Error: " + exception.getMessage();
        }
    }

    public void clearMemory() {
        chatMemory.clear(CONVERSATION_ID);
        history.clear();
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
