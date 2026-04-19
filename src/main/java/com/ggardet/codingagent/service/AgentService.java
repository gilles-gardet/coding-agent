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

    private static final String PLAN_MODE_INSTRUCTIONS = """

## PLAN MODE ACTIVE

You are in PLAN MODE. Your task is to analyze the request and produce a detailed, structured implementation plan.
STRICT CONSTRAINT: DO NOT call any tools. DO NOT modify files. DO NOT run shell commands. DO NOT perform any action.
Write a clear, numbered list of all steps needed to fulfill the request, with enough detail that each step can be executed without further clarification.
End your response by telling the user the plan is ready and asking whether they would like you to implement it.
""";

    private static final String EXECUTION_MODE_INSTRUCTIONS = """

## EXECUTION MODE ACTIVE

You are in EXECUTION MODE. A plan was previously described in this conversation.
Your task is to implement every step of that plan using all available tools NOW.
STRICT REQUIREMENT: You MUST call tools to create/modify files and run commands.
Generating text that describes code without calling tools is a failure.
Start immediately with tool calls — do not introduce or summarize, just execute.
""";

    private final String sessionId = UUID.randomUUID().toString();
    private final Map<String, Object> workingDirContext = Map.of("workingDir", System.getProperty("user.dir"));
    private volatile boolean planMode = false;
    private volatile boolean executionAfterPlan = false;

    public AgentService(
            final @Value("classpath:/prompts/system.st") Resource systemPromptResource,
            final ChatClient chatClient,
            final ChatMemory chatMemory) {
        this.systemPromptResource = systemPromptResource;
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    public Flux<String> streamChat(final String message) {
        final var systemPrompt = buildSystemPrompt();
        executionAfterPlan = false;
        return chatClient.prompt(message)
                .system(systemPrompt)
                .advisors(advisorSpec -> advisorSpec.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId))
                .toolContext(workingDirContext)
                .stream()
                .content();
    }

    public void clearMemory() {
        chatMemory.clear(sessionId);
    }

    public boolean togglePlanMode() {
        planMode = !planMode;
        return planMode;
    }

    public void activateExecutionAfterPlan() {
        planMode = false;
        executionAfterPlan = true;
    }

    public boolean isPlanMode() {
        return planMode;
    }

    private String buildSystemPrompt() {
        final var base = new PromptTemplate(systemPromptResource).render(workingDirContext);
        if (planMode) {
            return base + PLAN_MODE_INSTRUCTIONS;
        }
        if (executionAfterPlan) {
            return base + EXECUTION_MODE_INSTRUCTIONS;
        }
        return base;
    }
}
