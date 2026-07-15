package com.ggardet.codingagent.agent.event;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/// A multicast event bus that carries human-readable tool-activity messages from tools and aspects
/// to the terminal UI, which subscribes to render them inline during a turn.
@Component
public class ToolEventSink {
    private final Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();

    /// Publishes a tool-activity message to all current subscribers on a best-effort basis.
    ///
    /// @param event the message to emit
    public void emit(final String event) {
        sink.tryEmitNext(event);
    }

    /// Returns the stream of emitted tool-activity messages.
    ///
    /// @return a flux the UI can subscribe to for tool events
    public Flux<String> events() {
        return sink.asFlux();
    }
}
