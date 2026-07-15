package com.ggardet.codingagent.agent.loop;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/// Caps the number of tool-call iterations within a single agent turn. When the cap is
/// reached, a stop instruction is appended so the model produces a final answer instead of
/// calling more tools.
///
/// The counter is a shared [AtomicInteger] rather than a `ThreadLocal` because the streaming
/// tool-call loop recurses across reactive schedulers (bounded-elastic), so a thread-bound
/// counter would not survive the thread hops. The agent runs one turn at a time, so a single
/// shared counter is safe here.
@Component
public class MaxIterationsToolCallAdvisor extends ToolCallAdvisor {
    private final int maxIterations;
    private final AtomicInteger iterationCount = new AtomicInteger(0);
    private static final String STOP_MESSAGE = """
            You have reached the maximum allowed number of tool call iterations.
            Please provide your final answer now based on the information gathered so far,
            without calling any more tools.
            """;

    /// Creates the advisor.
    ///
    /// @param toolCallingManager the manager that executes tool calls within the loop
    /// @param maxIterations the maximum number of tool-call iterations allowed per turn
    public MaxIterationsToolCallAdvisor(
            final ToolCallingManager toolCallingManager,
            final @Value("${coding-agent.max-iterations:10}") int maxIterations) {
        super(toolCallingManager, Ordered.LOWEST_PRECEDENCE - 1, true);
        this.maxIterations = maxIterations;
    }

    /// Resets the iteration counter at the start of a blocking (non-streaming) loop.
    ///
    /// @param request the incoming request
    /// @param chain the call advisor chain
    /// @return the request, unchanged
    @Override
    protected @NonNull ChatClientRequest doInitializeLoop(
            final @NonNull ChatClientRequest request,
            final @NonNull CallAdvisorChain chain
    ) {
        iterationCount.set(0);
        return request;
    }

    /// Enforces the iteration cap before each blocking (non-streaming) model call.
    ///
    /// @param request the request about to be sent
    /// @param chain the call advisor chain
    /// @return the request, possibly with a stop instruction appended
    @Override
    protected @NonNull ChatClientRequest doBeforeCall(
            final @NonNull ChatClientRequest request,
            final @NonNull CallAdvisorChain chain
    ) {
        return enforceLimit(request);
    }

    /// Resets the iteration counter at the start of a streaming loop.
    ///
    /// @param request the incoming request
    /// @param chain the stream advisor chain
    /// @return the request, unchanged
    @Override
    protected @NonNull ChatClientRequest doInitializeLoopStream(
            final @NonNull ChatClientRequest request,
            final @NonNull StreamAdvisorChain chain
    ) {
        iterationCount.set(0);
        return request;
    }

    /// Enforces the iteration cap before each streaming model call.
    ///
    /// @param request the request about to be sent
    /// @param chain the stream advisor chain
    /// @return the request, possibly with a stop instruction appended
    @Override
    protected @NonNull ChatClientRequest doBeforeStream(
            final @NonNull ChatClientRequest request,
            final @NonNull StreamAdvisorChain chain
    ) {
        return enforceLimit(request);
    }

    /// Increments the iteration counter and appends the stop instruction once the cap is exceeded.
    ///
    /// @param request the request about to be sent
    /// @return the original request, or a copy with the stop instruction appended
    private ChatClientRequest enforceLimit(final ChatClientRequest request) {
        if (iterationCount.incrementAndGet() > maxIterations) {
            return appendStopMessage(request);
        }
        return request;
    }

    /// Returns a copy of the request with the stop instruction appended as a user message.
    ///
    /// @param request the request to augment
    /// @return a new request carrying the stop instruction
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
