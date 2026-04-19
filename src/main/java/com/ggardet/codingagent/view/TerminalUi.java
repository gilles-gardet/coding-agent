package com.ggardet.codingagent.view;

import com.ggardet.codingagent.logging.ToolEventSink;
import com.ggardet.codingagent.service.AgentService;
import reactor.core.Disposable;
import dev.tamboui.style.Overflow;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.elements.SpinnerElement;
import dev.tamboui.toolkit.elements.TextElement;
import dev.tamboui.toolkit.elements.TextInputElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.spinner.SpinnerStyle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.length;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spinner;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

@Component
public class TerminalUi extends ToolkitApp {
    private enum MessageType { USER, AGENT, SYSTEM }
    private record Message(MessageType type, String content) {}
    private final AgentService agentService;
    private final ToolEventSink toolEventSink;
    private final TextInputState inputState = new TextInputState();
    private final CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> streamingLines = new CopyOnWriteArrayList<>();
    private final StringBuilder streamingLineBuffer = new StringBuilder();
    private final ListElement<Message> historyList = new ListElement<>();
    private final SpinnerElement loadingSpinner = spinner(SpinnerStyle.DOTS, "Thinking...");
    private volatile String currentStreamingLine = "";
    private volatile boolean streaming = false;
    private volatile boolean ctrlCPressedOnce = false;
    private volatile long ctrlCFirstPressTime = 0;
    private volatile Disposable toolEventSubscription;
    private final TextInputElement inputField = textInput(inputState)
            .placeholder("Type a message and press Enter...")
            .focusable()
            .rounded()
            .onSubmit(this::sendMessage)
            .onKeyEvent(event -> {
                if (event.code() == KeyCode.CHAR && !event.hasCtrl() && !event.hasAlt()) {
                    final var c = event.character();
                    if (c > 127) {
                        inputState.insert(c);
                        return EventResult.HANDLED;
                    }
                }
                return EventResult.UNHANDLED;
            });

    private static final int BOTTOM_HEIGHT = 6;
    private static final String HINTS = " Enter: send  Ctrl+C: clear/quit  Ctrl+L: clear  Ctrl+P: toggle plan mode";

    public TerminalUi(final AgentService agentService, final ToolEventSink toolEventSink) {
        this.agentService = agentService;
        this.toolEventSink = toolEventSink;
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();
    }

    @Override
    protected Element render() {
        final var displayMessages = new ArrayList<>(List.copyOf(messages));
        if (streaming) {
            for (final var line : List.copyOf(streamingLines)) {
                displayMessages.add(new Message(MessageType.AGENT, line));
            }
            if (!currentStreamingLine.isEmpty()) {
                displayMessages.add(new Message(MessageType.AGENT, currentStreamingLine + "▊"));
            }
        }
        historyList.data(displayMessages, msg -> {
            final var prefix = msg.type() == MessageType.USER ? "> " : "  ";
            final var element = new TextElement(prefix + msg.content()).overflow(Overflow.WRAP_WORD);
            return switch (msg.type()) {
                case USER -> element.green();
                case AGENT -> element.white();
                case SYSTEM -> element.dim();
            };
        });
        final var activeInput = streaming ? loadingSpinner : inputField;
        final var modeLabel = agentService.isPlanMode() ? text(" [PLAN MODE]").yellow() : text(" [EXECUTE MODE]").green();
        final var hintsText = text(HINTS).dim();
        final var bottom = panel(
                column(activeInput, row(hintsText, modeLabel)).spacing(0)
        ).borderless();
        return dock()
                .center(historyList.displayOnly().stickyScroll().scrollbar())
                .bottom(bottom, length(BOTTOM_HEIGHT))
                .focusable()
                .onKeyEvent(event -> {
                    if (event.hasCtrl() && event.isChar('c')) {
                        final var now = System.currentTimeMillis();
                        if (ctrlCPressedOnce && now - ctrlCFirstPressTime <= 2000) {
                            quit();
                        } else {
                            ctrlCPressedOnce = true;
                            ctrlCFirstPressTime = now;
                            inputState.clear();
                        }
                        return EventResult.HANDLED;
                    }
                    ctrlCPressedOnce = false;
                    if (event.hasCtrl() && event.isChar('l')) {
                        clearConversation();
                        return EventResult.HANDLED;
                    }
                    if (event.hasCtrl() && event.isChar('p')) {
                        togglePlanMode();
                        return EventResult.HANDLED;
                    }
                    return historyList.handleKeyEvent(event, false);
                });
    }

    @Override
    protected void onStart() {
        final var content = "Coding Agent ready. Type a message below.";
        final var message = new Message(MessageType.SYSTEM, content);
        messages.add(message);
    }

    private void sendMessage() {
        final var message = inputState.text().trim();
        if (message.isEmpty() || streaming) {
            return;
        }
        inputState.clear();
        messages.add(new Message(MessageType.USER, message));
        startStreaming(message);
    }

    private void startStreaming(final String message) {
        streaming = true;
        streamingLines.clear();
        streamingLineBuffer.setLength(0);
        currentStreamingLine = "";
        toolEventSubscription = toolEventSink.events()
                .subscribe(event -> messages.add(new Message(MessageType.SYSTEM, event)));
        agentService.streamChat(message)
                .doOnNext(this::handleStreamingToken)
                .doOnComplete(this::finalizeStreaming)
                .doOnError(this::handleStreamingError)
                .subscribe();
    }

    private void implementPlan() {
        agentService.activateExecutionAfterPlan();
        messages.add(new Message(MessageType.SYSTEM, "Switched to EXECUTE MODE — implementing the plan..."));
        startStreaming("Implement every step of the plan you just described. Use all necessary tools to actually modify files, run commands, and make all required changes. Do not restate the plan — execute it now.");
    }

    private void handleStreamingToken(final String token) {
        streamingLineBuffer.append(token);
        final var content = streamingLineBuffer.toString();
        final var lastNewline = content.lastIndexOf('\n');
        if (lastNewline < 0) {
            currentStreamingLine = content;
            return;
        }
        final var lines = Arrays.asList(content.substring(0, lastNewline).split("\n", -1));
        streamingLines.addAll(lines);
        streamingLineBuffer.delete(0, lastNewline + 1);
        currentStreamingLine = streamingLineBuffer.toString();
    }

    private void finalizeStreaming() {
        disposeToolEventSubscription();
        if (!streamingLineBuffer.isEmpty()) {
            streamingLines.add(streamingLineBuffer.toString());
        }
        for (final var line : streamingLines) {
            messages.add(new Message(MessageType.AGENT, line));
        }
        streamingLines.clear();
        streamingLineBuffer.setLength(0);
        currentStreamingLine = "";
        streaming = false;
        if (agentService.isPlanMode()) {
            implementPlan();
        }
    }

    private void handleStreamingError(final Throwable error) {
        disposeToolEventSubscription();
        final var detail = extractErrorDetail(error);
        messages.add(new Message(MessageType.AGENT, "Error: " + detail));
        streamingLines.clear();
        streamingLineBuffer.setLength(0);
        currentStreamingLine = "";
        streaming = false;
    }

    private String extractErrorDetail(final Throwable error) {
        var cause = error;
        while (cause != null) {
            if (cause instanceof org.springframework.web.reactive.function.client.WebClientResponseException webEx) {
                return webEx.getStatusCode() + " — " + webEx.getResponseBodyAsString();
            }
            cause = cause.getCause();
        }
        return error.getMessage();
    }

    private void togglePlanMode() {
        final var newMode = agentService.togglePlanMode();
        final var modeMessage = newMode
                ? "Switched to PLAN MODE — agent will plan without executing actions"
                : "Switched to EXECUTE MODE — agent will execute actions normally";
        messages.add(new Message(MessageType.SYSTEM, modeMessage));
    }

    private void clearConversation() {
        disposeToolEventSubscription();
        agentService.clearMemory();
        messages.clear();
        streamingLines.clear();
        streamingLineBuffer.setLength(0);
        currentStreamingLine = "";
        streaming = false;
    }

    private void disposeToolEventSubscription() {
        if (toolEventSubscription != null && !toolEventSubscription.isDisposed()) {
            toolEventSubscription.dispose();
        }
    }
}
