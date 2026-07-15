package com.ggardet.codingagent.coding.tui;

import com.ggardet.codingagent.coding.approval.CommandApprovalService;
import com.ggardet.codingagent.coding.command.SlashCommand;
import com.ggardet.codingagent.coding.history.InputHistory;
import com.ggardet.codingagent.agent.event.ToolEventSink;
import com.ggardet.codingagent.agent.AgentService;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.length;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spinner;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

/// The terminal user interface. Renders the conversation, streams the agent's response, drives the
/// input field with slash-command autocompletion and history navigation, handles keyboard shortcuts
/// (cancel, plan mode, clear, quit), and prompts for approval of destructive commands.
@Component
public class TerminalUi extends ToolkitApp {
    /// The origin of a displayed message, used to style it.
    private enum MessageType { USER, AGENT, SYSTEM }

    /// A single line displayed in the conversation view.
    ///
    /// @param type the message origin
    /// @param content the text to display
    private record Message(MessageType type, String content) {}
    private final AgentService agentService;
    private final ToolEventSink toolEventSink;
    private final InputHistory inputHistory;
    private final CommandApprovalService approvalService;
    private final TextInputState inputState = new TextInputState();
    private final CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> streamingLines = new CopyOnWriteArrayList<>();
    private final StringBuilder streamingLineBuffer = new StringBuilder();
    private final ListElement<Message> historyList = new ListElement<>();
    private final SpinnerElement loadingSpinner = spinner(SpinnerStyle.DOTS, "Thinking...");
    private volatile String currentStreamingLine = "";
    private volatile boolean streaming = false;
    private volatile boolean planReady = false;
    private volatile int selectedSuggestion = 0;
    private volatile boolean ctrlCPressedOnce = false;
    private volatile long ctrlCFirstPressTime = 0;
    private volatile Disposable toolEventSubscription;
    private volatile Disposable streamSubscription;
    private volatile CommandApprovalService.ApprovalRequest pendingApproval;
    private final TextInputElement inputField;

    private static final int BOTTOM_HEIGHT = 6;
    private static final String HINTS = " Enter: send  /: commands  Esc: cancel  Ctrl+C: quit  Ctrl+L: clear  Ctrl+P: plan";

    /// Creates the terminal UI and wires the input field's submit, autocompletion, history, and
    /// unicode-input handlers.
    ///
    /// @param agentService the agent orchestration service
    /// @param toolEventSink the source of tool-activity events shown during a turn
    /// @param inputHistory the persistent input history
    /// @param approvalService the destructive-command approval mediator
    public TerminalUi(final AgentService agentService, final ToolEventSink toolEventSink, final InputHistory inputHistory, final CommandApprovalService approvalService) {
        this.agentService = agentService;
        this.toolEventSink = toolEventSink;
        this.inputHistory = inputHistory;
        this.approvalService = approvalService;
        this.inputField = textInput(inputState)
                .placeholder("Type a message and press Enter...")
                .focusable()
                .rounded()
                .onSubmit(this::sendMessage)
                .onKeyEvent(event -> {
                    final var suggestions = currentSuggestions();
                    if (!suggestions.isEmpty() && !event.hasCtrl() && !event.hasAlt()) {
                        if (event.code() == KeyCode.UP) {
                            selectedSuggestion = Math.floorMod(selectedSuggestion - 1, suggestions.size());
                            return EventResult.HANDLED;
                        }
                        if (event.code() == KeyCode.DOWN) {
                            selectedSuggestion = Math.floorMod(selectedSuggestion + 1, suggestions.size());
                            return EventResult.HANDLED;
                        }
                        if (event.code() == KeyCode.TAB) {
                            return completeSuggestion(suggestions);
                        }
                    }
                    if (event.code() == KeyCode.UP && !event.hasCtrl() && !event.hasAlt()) {
                        return navigateHistoryUp();
                    }
                    if (event.code() == KeyCode.DOWN && !event.hasCtrl() && !event.hasAlt()) {
                        return navigateHistoryDown();
                    }
                    if (event.code() == KeyCode.CHAR && !event.hasCtrl() && !event.hasAlt()) {
                        final var c = event.character();
                        if (c > 127) {
                            inputState.insert(c);
                            return EventResult.HANDLED;
                        }
                    }
                    return EventResult.UNHANDLED;
                });
    }

    /// Configures the TUI runtime, setting the render tick rate.
    ///
    /// @return the TUI configuration
    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();
    }

    /// Builds the full UI tree for the current frame: the scrolling conversation view plus the
    /// bottom area (command suggestions, approval prompt, input/spinner, and hints), and installs
    /// the top-level key handler for shortcuts and approval decisions.
    ///
    /// @return the root element to render
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
        final var suggestions = currentSuggestions();
        final var bottomChildren = new ArrayList<Element>();
        if (!suggestions.isEmpty()) {
            final var selected = Math.min(selectedSuggestion, suggestions.size() - 1);
            for (var index = 0; index < suggestions.size(); index++) {
                final var command = suggestions.get(index);
                final var line = text("  /" + command.commandName() + "  " + command.description());
                bottomChildren.add(index == selected ? line.cyan().bold() : line.dim());
            }
        }
        final var approval = pendingApproval;
        if (Objects.nonNull(approval)) {
            bottomChildren.add(text("⚠ Approve destructive command?  [y] run   [n] deny").yellow().bold());
            bottomChildren.add(text("  " + approval.command()).dim());
        }
        bottomChildren.add(activeInput);
        bottomChildren.add(row(hintsText, modeLabel));
        final var bottom = panel(column(bottomChildren.toArray(Element[]::new)).spacing(0)).borderless();
        final var extraLines = suggestions.size() + (Objects.nonNull(approval) ? 2 : 0);
        return dock()
                .center(historyList.displayOnly().stickyScroll().scrollbar())
                .bottom(bottom, length(BOTTOM_HEIGHT + extraLines))
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
                    final var approvalRequest = pendingApproval;
                    if (Objects.nonNull(approvalRequest)) {
                        if (event.isChar('y') || event.isChar('Y')) {
                            resolveApproval(approvalRequest, true);
                        } else if (event.isChar('n') || event.isChar('N')
                                || event.code() == KeyCode.ESCAPE || event.code() == KeyCode.ENTER) {
                            resolveApproval(approvalRequest, false);
                        }
                        return EventResult.HANDLED;
                    }
                    if (event.code() == KeyCode.ESCAPE && streaming) {
                        cancelStreaming();
                        return EventResult.HANDLED;
                    }
                    if (event.hasCtrl() && event.isChar('l')) {
                        clearConversation();
                        return EventResult.HANDLED;
                    }
                    if (event.hasCtrl() && event.isChar('p')) {
                        togglePlanMode();
                        return EventResult.HANDLED;
                    }
                    if (event.hasCtrl() && event.isChar('y') && planReady && !streaming) {
                        planReady = false;
                        implementPlan();
                        return EventResult.HANDLED;
                    }
                    return historyList.handleKeyEvent(event, false);
                });
    }

    /// Shows the welcome message and subscribes to approval requests so a pending one surfaces in
    /// the UI.
    @Override
    protected void onStart() {
        final var content = "Coding Agent ready. Type a message below.";
        final var message = new Message(MessageType.SYSTEM, content);
        messages.add(message);
        approvalService.requests().subscribe(request -> pendingApproval = request);
    }

    /// Replaces the input with the previous (older) history entry.
    ///
    /// @return [EventResult#HANDLED]
    private EventResult navigateHistoryUp() {
        final var entry = inputHistory.navigateUp(inputState.text());
        inputState.setText(entry);
        inputState.moveCursorToEnd();
        return EventResult.HANDLED;
    }

    /// Replaces the input with the next (more recent) history entry, or the restored draft.
    ///
    /// @return [EventResult#HANDLED]
    private EventResult navigateHistoryDown() {
        final var entry = inputHistory.navigateDown();
        inputState.setText(entry);
        inputState.moveCursorToEnd();
        return EventResult.HANDLED;
    }

    /// Handles input submission: dispatches slash commands, otherwise records the message and starts
    /// streaming the agent's response. Ignores blank input and input while a turn is streaming.
    private void sendMessage() {
        final var message = inputState.text().trim();
        if (message.isEmpty() || streaming) {
            return;
        }
        if (message.startsWith("/")) {
            handleCommand(message);
            return;
        }
        inputState.clear();
        inputHistory.add(message);
        planReady = false;
        messages.add(new Message(MessageType.USER, message));
        startStreaming(message);
    }

    /// Computes the slash-command suggestions for the current input: the commands whose names match
    /// the typed prefix, shown only while typing a command name (before any space) and not while
    /// streaming.
    ///
    /// @return the matching commands, or an empty list when no suggestions apply
    private List<SlashCommand> currentSuggestions() {
        if (streaming) {
            return List.of();
        }
        final var text = inputState.text();
        if (!text.startsWith("/") || text.contains(" ")) {
            return List.of();
        }
        return SlashCommand.matching(text.substring(1).toLowerCase());
    }

    /// Fills the input with the highlighted suggestion, followed by a trailing space so arguments
    /// can be typed.
    ///
    /// @param suggestions the current suggestion list
    /// @return [EventResult#HANDLED]
    private EventResult completeSuggestion(final List<SlashCommand> suggestions) {
        final var index = Math.min(selectedSuggestion, suggestions.size() - 1);
        inputState.setText("/" + suggestions.get(index).commandName() + " ");
        inputState.moveCursorToEnd();
        return EventResult.HANDLED;
    }

    /// Parses a slash-command input into a command name and arguments, resolves the command, and
    /// executes it — showing help for a bare slash and an error for an unknown command.
    ///
    /// @param rawInput the full input starting with a slash
    private void handleCommand(final String rawInput) {
        final var withoutSlash = rawInput.substring(1);
        final var spaceIndex = withoutSlash.indexOf(' ');
        final var name = (spaceIndex < 0 ? withoutSlash : withoutSlash.substring(0, spaceIndex)).toLowerCase();
        final var arguments = spaceIndex < 0 ? "" : withoutSlash.substring(spaceIndex + 1);
        inputState.clear();
        inputHistory.add(rawInput);
        if (name.isEmpty()) {
            showHelp();
            return;
        }
        final var resolved = resolveCommand(name);
        if (resolved.isEmpty()) {
            messages.add(new Message(MessageType.SYSTEM, "Unknown command: /" + name + " — type / to see available commands"));
            return;
        }
        executeCommand(resolved.get(), arguments);
    }

    /// Resolves a typed command name to a command: an exact name match wins, otherwise the
    /// highlighted prefix match is used.
    ///
    /// @param name the typed command name
    /// @return the resolved command, or empty if no name matches
    private Optional<SlashCommand> resolveCommand(final String name) {
        final var exact = SlashCommand.byName(name);
        if (exact.isPresent()) {
            return exact;
        }
        final var matches = SlashCommand.matching(name);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matches.get(Math.min(selectedSuggestion, matches.size() - 1)));
    }

    /// Executes a resolved command: local commands (plan, clear, help) act directly, while
    /// prompt-backed commands echo the command and stream the expanded prompt to the agent.
    ///
    /// @param command the command to run
    /// @param arguments the raw arguments typed after the command
    private void executeCommand(final SlashCommand command, final String arguments) {
        selectedSuggestion = 0;
        switch (command) {
            case PLAN -> togglePlanMode();
            case CLEAR -> clearConversation();
            case HELP -> showHelp();
            default -> {
                planReady = false;
                final var label = "/" + command.commandName() + (arguments.isBlank() ? "" : " " + arguments.trim());
                messages.add(new Message(MessageType.USER, label));
                startStreaming(command.expand(arguments));
            }
        }
    }

    /// Adds a system message listing every available slash command and its description.
    private void showHelp() {
        final var help = new StringBuilder("Available commands:");
        for (final var command : SlashCommand.values()) {
            help.append("\n  /").append(command.commandName()).append("  —  ").append(command.description());
        }
        messages.add(new Message(MessageType.SYSTEM, help.toString()));
    }

    /// Starts streaming the agent's response to a message, subscribing to tool events and wiring the
    /// token, completion, and error handlers.
    ///
    /// @param message the message (or expanded command prompt) to send to the agent
    private void startStreaming(final String message) {
        streaming = true;
        streamingLines.clear();
        streamingLineBuffer.setLength(0);
        currentStreamingLine = "";
        toolEventSubscription = toolEventSink.events()
                .subscribe(event -> messages.add(new Message(MessageType.SYSTEM, event)));
        streamSubscription = agentService.streamChat(message)
                .doOnNext(this::handleStreamingToken)
                .doOnComplete(this::finalizeStreaming)
                .doOnError(this::handleStreamingError)
                .subscribe();
    }

    /// Cancels the in-progress turn: denies any pending approval, disposes the subscriptions, flushes
    /// the partial output, and marks the turn cancelled.
    private void cancelStreaming() {
        denyPendingApproval();
        if (Objects.nonNull(streamSubscription) && !streamSubscription.isDisposed()) {
            streamSubscription.dispose();
        }
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
        messages.add(new Message(MessageType.SYSTEM, "Cancelled."));
    }

    /// Resolves a pending approval with the user's decision, unblocking the waiting tool thread and
    /// recording the outcome as a system message.
    ///
    /// @param approval the pending approval request
    /// @param approved `true` to allow the command, `false` to deny it
    private void resolveApproval(final CommandApprovalService.ApprovalRequest approval, final boolean approved) {
        pendingApproval = null;
        approval.decision().complete(approved);
        messages.add(new Message(MessageType.SYSTEM, (approved ? "Approved: " : "Denied: ") + approval.command()));
    }

    /// Denies any pending approval without a message, used when cancelling or clearing so the waiting
    /// tool thread is released.
    private void denyPendingApproval() {
        final var approval = pendingApproval;
        if (Objects.nonNull(approval)) {
            pendingApproval = null;
            approval.decision().complete(false);
        }
    }

    /// Switches to execution mode and streams a request for the agent to implement the plan it just
    /// produced.
    private void implementPlan() {
        agentService.activateExecutionAfterPlan();
        messages.add(new Message(MessageType.SYSTEM, "Switched to EXECUTE MODE — implementing the plan..."));
        startStreaming("Implement every step of the plan you just described. Use all necessary tools to actually modify files, run commands, and make all required changes. Do not restate the plan — execute it now.");
    }

    /// Accumulates a streamed token, splitting completed lines out of the buffer so they render as
    /// finished lines while the trailing partial line renders with a cursor.
    ///
    /// @param token the next streamed content token
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

    /// Finalizes a completed turn: flushes remaining output into the conversation, ends streaming,
    /// and — when in plan mode — marks the plan ready for the user to approve implementation.
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
            planReady = true;
            messages.add(new Message(MessageType.SYSTEM, "Plan ready — press Ctrl+Y to implement, or keep chatting to refine it."));
        }
    }

    /// Handles a streaming failure by showing the extracted error detail and resetting streaming
    /// state.
    ///
    /// @param error the error that terminated the stream
    private void handleStreamingError(final Throwable error) {
        disposeToolEventSubscription();
        final var detail = extractErrorDetail(error);
        messages.add(new Message(MessageType.AGENT, "Error: " + detail));
        streamingLines.clear();
        streamingLineBuffer.setLength(0);
        currentStreamingLine = "";
        streaming = false;
    }

    /// Extracts a human-readable detail from an error, preferring the status and body of a web-client
    /// response exception found anywhere in the cause chain.
    ///
    /// @param error the error to inspect
    /// @return a message describing the error
    private String extractErrorDetail(final Throwable error) {
        var cause = error;
        while (Objects.nonNull(cause)) {
            if (cause instanceof org.springframework.web.reactive.function.client.WebClientResponseException webEx) {
                return webEx.getStatusCode() + " — " + webEx.getResponseBodyAsString();
            }
            cause = cause.getCause();
        }
        return error.getMessage();
    }

    /// Toggles plan mode and announces the new mode as a system message.
    private void togglePlanMode() {
        planReady = false;
        final var newMode = agentService.togglePlanMode();
        final var modeMessage = newMode
                ? "Switched to PLAN MODE — agent will plan without executing actions"
                : "Switched to EXECUTE MODE — agent will execute actions normally";
        messages.add(new Message(MessageType.SYSTEM, modeMessage));
    }

    /// Clears the conversation: denies any pending approval, disposes subscriptions, clears agent
    /// memory and all UI state, and resets input-history navigation.
    private void clearConversation() {
        denyPendingApproval();
        disposeToolEventSubscription();
        agentService.clearMemory();
        messages.clear();
        streamingLines.clear();
        streamingLineBuffer.setLength(0);
        currentStreamingLine = "";
        streaming = false;
        planReady = false;
        inputHistory.resetNavigation();
    }

    /// Disposes the tool-event subscription if it is active.
    private void disposeToolEventSubscription() {
        if (Objects.nonNull(toolEventSubscription) && !toolEventSubscription.isDisposed()) {
            toolEventSubscription.dispose();
        }
    }
}
