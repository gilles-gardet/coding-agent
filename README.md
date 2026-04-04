# Coding Agent

A CLI coding assistant powered by Spring AI and Spring Shell.  
It provides an interactive shell with access to file system tools, code search, and shell execution — all driven by an OpenAI language model.

## Requirements

- Java 25 (GraalVM for native compilation)
- Maven 3.9+
- An OpenAI API key

## Configuration

Export your OpenAI API key before running:

```bash
export OPENAI_API_KEY="sk-..."
```

## Running

```bash
mvn spring-boot:run
```

## Native compilation (GraalVM)

```bash
mvn -Pnative native:compile # should compile as "./target/coding-agent"
```

> **Note:** GraalVM JDK is required. Install it via SDKMAN: `sdk env install`

## Commands

| Command | Description |
|---------|-------------|
| `chat` | Enter interactive mode — send messages until you type `exit` or `quit` |
| `chat --message "<text>"` | Send a single message and return to the shell |
| `clear` | Clear the conversation memory |
| `history` | Display the conversation history for the current session |
| `help` | List all available commands |

## Skills

The agent has access to the following tools via `spring-ai-agent-utils`:

- **FileSystemTools** — read and write files
- **GrepTool** — search file contents with regex
- **GlobTool** — find files by pattern
- **ShellTools** — execute shell commands

The working directory passed to tools is the directory from which the application is launched (`user.dir`).
