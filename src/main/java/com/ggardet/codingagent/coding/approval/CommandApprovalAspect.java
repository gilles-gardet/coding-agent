package com.ggardet.codingagent.coding.approval;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Gates destructive {@code rm} shell commands behind explicit user approval. Intercepts the
 * shell tool before execution and, when the command deletes files, blocks until the user
 * approves through {@link CommandApprovalService}. A denied command is never executed; the tool
 * returns a message telling the model to stop instead.
 */
@Aspect
@Component
public class CommandApprovalAspect {
    // ponytail: token heuristic, not a shell parser — catches direct `rm`, `find -exec rm`, and
    // `xargs rm`; obfuscated forms (aliases, eval, command substitution) can still slip through
    private static final Pattern DESTRUCTIVE_RM = Pattern.compile(
            "(?:^|[;&|\\n])\\s*rm\\b|(?:-exec|xargs)\\b[^\\n;]*\\brm\\b");
    private final CommandApprovalService approvalService;

    public CommandApprovalAspect(final CommandApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Around("execution(public String org.springaicommunity.agent.tools.ShellTools.bash(..)) && args(command, ..)")
    public Object gateShellCommand(final ProceedingJoinPoint joinPoint, final String command) throws Throwable {
        if (command != null && DESTRUCTIVE_RM.matcher(command).find() && !approvalService.requestApproval(command)) {
            return "Command denied by the user. Do not retry it; ask the user how to proceed.";
        }
        return joinPoint.proceed();
    }
}
