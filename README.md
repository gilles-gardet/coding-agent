# Coding Agent

> **Learning project.** This exists for me to learn how a coding-agent system is shaped — it is not
> a product and is not intended to be used, supported, or relied upon by anyone else. Expect rough
> edges, hardcoded choices, and breaking changes without notice.

A terminal coding assistant powered by [Spring AI](https://spring.io/projects/spring-ai) and an OpenAI model, with an interactive TUI built on tamboui (`dev.tamboui`). It can read and write files, search code, run shell commands, search the web, and use on-disk skills — driven by a ReAct tool-calling loop.

The codebase is split into two layers to keep the agent "brain" separate from the app that wraps it:

- **`agent`** — the portable brain: the tool-calling loop, tool-observation events, session memory, and orchestration.
- **`coding`** — the app: the TUI, slash commands, input history, provider/tool wiring, skills, and the destructive-command approval gate.

## Requirements

- Java 25 — GraalVM for native compilation (`sdk env install` picks it up from `.sdkmanrc`)
- Maven 3.9+
- An OpenAI API key
- A [Tavily](https://tavily.com) API key (for the web-search tool)

## Configuration

Provide the API keys as environment variables:

```bash
export OPENAI_API_KEY="sk-..."
export TAVILY_API_KEY="tvly-..."
```

If a required key is missing, the agent prompts for it interactively on startup.

The chat model (`gpt-4o-mini` by default) and the tool-call iteration cap are configurable in `src/main/resources/application.yaml`:

```yaml
spring.ai.openai.chat.options.model: gpt-4o-mini
coding-agent.max-iterations: 10
```

## Running

On the JVM:

```bash
mvn spring-boot:run
```

As a native binary (GraalVM):

```bash
mvn -Pnative native:compile   # produces ./target/coding-agent
./target/coding-agent
```

## Using the TUI

Type a message and press **Enter**. Tool activity streams inline as the agent works.

| Key | Action |
|-----|--------|
| `Enter` | Send the message |
| `/` | Start a slash command (shows autocompletion) |
| `Tab` / `↑` `↓` | Complete / navigate command suggestions |
| `↑` `↓` | Browse input history (when no suggestions are open) |
| `Esc` | Cancel the in-progress turn |
| `Ctrl+P` | Toggle plan mode |
| `Ctrl+Y` | Implement the plan (once one is ready) |
| `Ctrl+L` | Clear the conversation |
| `Ctrl+C` | Clear the input, then quit on a second press |

### Slash commands

| Command | Description |
|---------|-------------|
| `/review` | Review the current code changes for bugs and improvements |
| `/analyze` | Analyze the codebase architecture and quality |
| `/explain <subject>` | Explain a file, function, or concept |
| `/test` | Run the test suite and fix failures |
| `/commit` | Stage changes and create a git commit |
| `/plan` | Toggle plan mode (plan without executing) |
| `/clear` | Clear the conversation |
| `/help` | List available commands |

### Plan mode

In plan mode the agent produces a step-by-step plan without touching anything and waits. Press **Ctrl+Y** to have it implement the plan, or keep chatting to refine it.

## Tools

Wired via `spring-ai-agent-utils`:

- **FileSystemTools** — read and write files
- **GrepTool** — search file contents with regex
- **GlobTool** — find files by pattern
- **ShellTools** — execute shell commands
- **WebSearch** (Tavily) — search the web
- **TodoWrite** — maintain a task plan (rendered in the UI)
- **Skill** — load and apply on-disk skills

The working directory passed to tools is the directory the application is launched from (`user.dir`).

### Destructive-command approval

Before the shell tool runs a destructive `rm` command, the agent pauses and asks for confirmation in the TUI (`[y] run` / `[n] deny`). A denied command is never executed. Detection is a heuristic (direct `rm`, `find -exec rm`, `xargs rm`), not a full shell parser.

## Skills

Skills are markdown playbooks the agent can load on demand. Bundled skills are seeded to `~/.agent/skills` on first run and read from there (git, tdd, and one per core shell tool: fd, grep, ls, cat, mv, cp, rm). Add your own by dropping a `SKILL.md` under `~/.agent/skills/<name>/`.

## Memory

The agent keeps cross-session memory as files under `~/.agent/memories`, indexed by `MEMORY.md`. Within a session, conversation history is compacted automatically once it grows past a turn threshold.
