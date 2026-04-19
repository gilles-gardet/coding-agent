package com.ggardet.codingagent.logging;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class ToolEventSink {
    private final Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();

    public void emit(final String event) {
        sink.tryEmitNext(event);
    }

    public Flux<String> events() {
        return sink.asFlux();
    }
}
