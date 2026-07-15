package com.ggardet.codingagent.coding.approval;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/// Mediates human-in-the-loop approval of dangerous shell commands between the tool-execution
/// thread and the terminal UI. The tool thread blocks on [#requestApproval(String)] until
/// the UI resolves the emitted [ApprovalRequest], so a command never runs before the user
/// has decided.
@Component
public class CommandApprovalService {
    /// A pending approval for a single command.
    ///
    /// @param command the shell command awaiting approval
    /// @param decision the future the UI completes with the user's decision (`true` to allow)
    public record ApprovalRequest(String command, CompletableFuture<Boolean> decision) {}

    private static final long APPROVAL_TIMEOUT_SECONDS = 300;
    private final Sinks.Many<ApprovalRequest> requests = Sinks.many().multicast().directBestEffort();

    /// Blocks the calling (tool) thread until the UI approves or denies the command. A missing
    /// decision within the timeout is treated as a denial so a stalled UI cannot let the command
    /// through.
    ///
    /// @param command the shell command to seek approval for
    /// @return `true` if the user approved, `false` if denied or the wait timed out
    public boolean requestApproval(final String command) {
        final var request = new ApprovalRequest(command, new CompletableFuture<>());
        requests.tryEmitNext(request);
        try {
            return request.decision().get(APPROVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final Exception exception) {
            return false;
        }
    }

    /// Returns the stream of pending approval requests for the UI to subscribe to.
    ///
    /// @return a flux of approval requests
    public Flux<ApprovalRequest> requests() {
        return requests.asFlux();
    }
}
