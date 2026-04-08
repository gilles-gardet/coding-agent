package com.ggardet.codingagent.tui;

import com.ggardet.codingagent.service.AgentService;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.length;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.spinner;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

@Component
public class CodingAgentTui extends ToolkitApp {
    private enum MessageType { USER, AGENT, SYSTEM }
    private record Message(MessageType type, String content) {}
    private final AgentService agentService;
    private final TextInputState inputState = new TextInputState();
    private final CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();
    private final ListElement<Message> historyList = new ListElement<>();
    private final SpinnerElement loadingSpinner = spinner(SpinnerStyle.DOTS, "Thinking...");
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
    private volatile boolean loading = false;

    private static final int BOTTOM_HEIGHT = 6;
    private static final String HINTS = " Enter: send  Ctrl+L: clear  Ctrl+C: quit";

    public CodingAgentTui(final AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();
    }

    @Override
    protected Element render() {
        historyList.data(List.copyOf(messages), msg -> {
            final var prefix = msg.type() == MessageType.USER ? "> " : "  ";
            final var element = new TextElement(prefix + msg.content()).overflow(Overflow.WRAP_WORD);
            return switch (msg.type()) {
                case USER -> element.green();
                case AGENT -> element.cyan();
                case SYSTEM -> element.dim();
            };
        });
        final var activeInput = loading ? loadingSpinner : inputField;
        final var bottom = panel(
                column(activeInput, text(HINTS).dim()).spacing(0)
        ).borderless();
        return dock()
                .center(historyList.displayOnly().stickyScroll().scrollbar())
                .bottom(bottom, length(BOTTOM_HEIGHT))
                .focusable()
                .onKeyEvent(event -> {
                    if (event.hasCtrl() && event.isChar('l')) {
                        clearConversation();
                        return EventResult.HANDLED;
                    }
                    return historyList.handleKeyEvent(event, false);
                });
    }

    @Override
    protected void onStart() {
        messages.add(new Message(MessageType.SYSTEM, "Coding Agent ready. Type a message below."));
    }

    private void sendMessage() {
        final var message = inputState.text().trim();
        if (message.isEmpty()) {
            return;
        }
        inputState.clear();
        messages.add(new Message(MessageType.USER, message));
        loading = true;
        Thread.ofVirtual().start(() -> {
            final var response = agentService.chat(message);
            for (final var line : response.split("\n", -1)) {
                messages.add(new Message(MessageType.AGENT, line));
            }
            loading = false;
        });
    }

    private void clearConversation() {
        agentService.clearMemory();
        messages.clear();
    }
}

