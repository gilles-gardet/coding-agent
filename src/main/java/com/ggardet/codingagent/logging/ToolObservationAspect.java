package com.ggardet.codingagent.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class ToolObservationAspect {
    private static final int MAX_ARG_LENGTH = 80;
    private final ToolEventSink toolEventSink;

    public ToolObservationAspect(final ToolEventSink toolEventSink) {
        this.toolEventSink = toolEventSink;
    }

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

    private static String truncate(final String text) {
        if (text.length() <= ToolObservationAspect.MAX_ARG_LENGTH) {
            return text;
        }
        return text.substring(0, ToolObservationAspect.MAX_ARG_LENGTH) + "…";
    }
}
