package com.ggardet.codingagent.coding.approval;

import java.util.concurrent.CompletableFuture;

/// A pending approval for a single command.
///
/// @param command the shell command awaiting approval
/// @param decision the future the UI completes with the user's decision (`true` to allow)
public record ApprovalRequest(String command, CompletableFuture<Boolean> decision) {
}
