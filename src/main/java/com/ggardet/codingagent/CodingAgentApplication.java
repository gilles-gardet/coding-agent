package com.ggardet.codingagent;

import com.ggardet.codingagent.hints.NativeRuntimeHints;
import com.ggardet.codingagent.view.TerminalUi;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints(NativeRuntimeHints.class)
@SpringBootApplication
public class CodingAgentApplication implements ApplicationRunner {
    private final TerminalUi terminalUi;

    public CodingAgentApplication(final TerminalUi terminalUi) {
        this.terminalUi = terminalUi;
    }

    static void main(final String[] args) {
        SpringApplication.run(CodingAgentApplication.class, args);
    }

    @Override
    public void run(final @NonNull ApplicationArguments args) throws Exception {
        terminalUi.run();
        System.exit(0);
    }
}

