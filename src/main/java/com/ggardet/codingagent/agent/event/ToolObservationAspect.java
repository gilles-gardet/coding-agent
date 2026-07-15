package com.ggardet.codingagent.agent.event;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/// Intercepts every tool invocation and emits a truncated, human-readable trace of the tool name
/// and arguments to the [ToolEventSink] so the UI can show tool activity as the agent works.
@Aspect
@Component
public class ToolObservationAspect {
    private static final int MAX_ARG_LENGTH = 80;
    private final ToolEventSink toolEventSink;

    /// Creates the aspect.
    ///
    /// @param toolEventSink the sink that tool traces are published to
    public ToolObservationAspect(final ToolEventSink toolEventSink) {
        this.toolEventSink = toolEventSink;
    }

    /// Emits a trace for the intercepted tool call, then proceeds with the actual invocation.
    ///
    /// @param joinPoint the intercepted tool-method invocation
    /// @return the value returned by the underlying tool method
    /// @throws Throwable whatever the underlying tool method throws
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object observeToolCall(final ProceedingJoinPoint joinPoint) throws Throwable {
        final var method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        final var annotation = method.getAnnotation(Tool.class);
        final var toolName = annotation.name().isEmpty() ? method.getName() : annotation.name();
        final var args = Arrays.stream(joinPoint.getArgs())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        toolEventSink.emit("🔧 [" + toolName + "] " + truncate(args));
        return joinPoint.proceed();
    }

    /// Truncates a string to [#MAX_ARG_LENGTH] characters, appending an ellipsis when shortened.
    ///
    /// @param text the text to truncate
    /// @return the original text, or a truncated copy ending with an ellipsis
    private static String truncate(final String text) {
        if (text.length() <= ToolObservationAspect.MAX_ARG_LENGTH) {
            return text;
        }
        return text.substring(0, ToolObservationAspect.MAX_ARG_LENGTH) + "…";
    }
}
