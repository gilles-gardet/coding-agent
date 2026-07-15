package com.ggardet.codingagent;

import com.ggardet.codingagent.coding.hints.NativeRuntimeHints;
import com.ggardet.codingagent.coding.tui.TerminalUi;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

/// Spring Boot entry point. Ensures the required API keys are present, boots the application
/// context, and runs the terminal UI as an [ApplicationRunner].
@ImportRuntimeHints(NativeRuntimeHints.class)
@SpringBootApplication
public class CodingAgentApplication implements ApplicationRunner {
    private final TerminalUi terminalUi;

    /// Creates the application runner.
    ///
    /// @param terminalUi the terminal UI to run once the context is ready
    public CodingAgentApplication(final TerminalUi terminalUi) {
        this.terminalUi = terminalUi;
    }

    /// Application entry point: resolves required API keys, then starts the Spring context.
    ///
    /// @param args the command-line arguments
    static void main(final String[] args) {
        ensureApiKey("OPENAI_API_KEY", "OpenAI API key (for the chat model)");
        ensureApiKey("TAVILY_API_KEY", "Tavily API key (for web search)");
        SpringApplication.run(CodingAgentApplication.class, args);
    }

    /// Ensures a required API key is available before the Spring context starts, since the
    /// configuration references it through `${...}` placeholders that fail context startup when
    /// unresolved. Missing keys are prompted for interactively and exposed as system properties
    /// so the environment can resolve them.
    ///
    /// @param name the environment variable / system property name of the key
    /// @param description a human-readable description shown in the interactive prompt
    private static void ensureApiKey(final String name, final String description) {
        if (StringUtils.hasText(System.getenv(name)) || StringUtils.hasText(System.getProperty(name))) {
            return;
        }
        final var console = System.console();
        final var prompt = name + " is not set. Enter your " + description + ": ";
        var value = promptForSecret(console, prompt);
        // Re-prompt only with an interactive console; a null console means EOF/non-tty, where
        // looping would spin forever — leave the key unset and let startup fail with a clear error.
        while (value.isBlank() && console != null) {
            value = promptForSecret(console, prompt);
        }
        if (!value.isBlank()) {
            System.setProperty(name, value);
        }
    }

    /// Reads a secret from the console (without echo when a console is available, otherwise from
    /// standard input).
    ///
    /// @param console the system console, or `null` when none is attached
    /// @param prompt the prompt text to display
    /// @return the entered value trimmed, or an empty string on EOF or read error
    private static String promptForSecret(final java.io.Console console, final String prompt) {
        if (console != null) {
            final var secret = console.readPassword(prompt);
            return Objects.isNull(secret) ? "" : new String(secret).trim();
        }
        System.out.print(prompt);
        System.out.flush();
        try {
            final var line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            return Objects.requireNonNullElse(line, "").trim();
        } catch (final IOException exception) {
            return "";
        }
    }

    /// Runs the terminal UI once the context is ready, then exits the process when it returns.
    ///
    /// @param args the parsed application arguments
    /// @throws Exception if the terminal UI fails to run
    @Override
    public void run(final @NonNull ApplicationArguments args) throws Exception {
        terminalUi.run();
        System.exit(0);
    }
}

