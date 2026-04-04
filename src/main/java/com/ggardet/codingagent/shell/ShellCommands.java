package com.ggardet.codingagent.shell;

import com.ggardet.codingagent.service.AgentService;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ShellCommands {
    private final AgentService agentService;
    private final LineReader lineReader;
    private final Terminal terminal;

    public ShellCommands(
            final AgentService agentService,
            final @Lazy LineReader lineReader,
            final @Lazy Terminal terminal
    ) {
        this.agentService = agentService;
        this.lineReader = lineReader;
        this.terminal = terminal;
    }

    @Command(name = "chat", description = "Chat with the coding agent. Omit message to enter interactive mode.")
    public String chat(@Option final String message) {
        if (Objects.nonNull(message)) {
            return agentService.chat(message);
        }
        terminal.writer().println("Interactive mode — type 'exit' or 'quit' to return to the shell.");
        terminal.writer().flush();
        while (true) {
            final var input = lineReader.readLine("> ").trim();
            if ("exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
                break;
            }
            terminal.writer().println(agentService.chat(input));
            terminal.writer().flush();
        }
        return "Exited interactive mode.";
    }

    @Command(name = "clear", description = "Clear the conversation memory.")
    public String clear() {
        agentService.clearMemory();
        return "Conversation memory cleared.";
    }

    @Command(name = "history", description = "Show the conversation history.")
    public String history() {
        final var entries = agentService.getHistory();
        if (entries.isEmpty()) {
            return "No conversation history yet.";
        }
        final var builder = new StringBuilder();
        for (var i = 0; i < entries.size(); i += 2) {
            builder.append("[").append((i / 2) + 1).append("] ").append(entries.get(i)).append("\n");
            if (i + 1 < entries.size()) {
                builder.append(entries.get(i + 1)).append("\n");
            }
        }
        return builder.toString().stripTrailing();
    }
}
