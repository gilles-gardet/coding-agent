package com.ggardet.codingagent.advisor;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class MaxIterationsToolCallAdvisor extends ToolCallAdvisor {
    private final int maxIterations;

    private final ThreadLocal<Integer> iterationCount = ThreadLocal.withInitial(() -> 0);
    private static final String STOP_MESSAGE = """
            You have reached the maximum allowed number of tool call iterations. \
            Please provide your final answer now based on the information gathered so far, \
            without calling any more tools.""";

    public MaxIterationsToolCallAdvisor(
            final ToolCallingManager toolCallingManager,
            final @Value("${coding-agent.max-iterations:10}") int maxIterations) {
        super(toolCallingManager, Ordered.LOWEST_PRECEDENCE, false);
        this.maxIterations = maxIterations;
    }

    @Override
    protected @NonNull ChatClientRequest doInitializeLoop(
            final @NonNull ChatClientRequest request,
            final @NonNull CallAdvisorChain chain
    ) {
        iterationCount.set(0);
        return request;
    }

    @Override
    protected @NonNull ChatClientRequest doBeforeCall(
            final @NonNull ChatClientRequest request,
            final @NonNull CallAdvisorChain chain
    ) {
        final var current = iterationCount.get() + 1;
        iterationCount.set(current);
        if (current > maxIterations) {
            return appendStopMessage(request);
        }
        return request;
    }

    @Override
    protected @NonNull ChatClientResponse doFinalizeLoop(
            final @NonNull ChatClientResponse response,
            final @NonNull CallAdvisorChain chain
    ) {
        iterationCount.remove();
        return response;
    }

    private ChatClientRequest appendStopMessage(final ChatClientRequest request) {
        final var instructions = new ArrayList<>(request.prompt().getInstructions());
        instructions.add(new UserMessage(STOP_MESSAGE));
        final var newPrompt = new Prompt(instructions, request.prompt().getOptions());
        return ChatClientRequest.builder()
                .prompt(newPrompt)
                .context(request.context())
                .build();
    }
}
