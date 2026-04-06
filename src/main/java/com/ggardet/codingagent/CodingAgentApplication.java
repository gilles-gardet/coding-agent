package com.ggardet.codingagent;

import com.ggardet.codingagent.config.AgentToolsRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints(AgentToolsRuntimeHints.class)
@SpringBootApplication
public class CodingAgentApplication {
    static void main(final String[] args) {
        SpringApplication.run(CodingAgentApplication.class, args);
    }
}
