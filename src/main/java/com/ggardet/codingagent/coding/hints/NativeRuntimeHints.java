package com.ggardet.codingagent.coding.hints;

import org.springaicommunity.agent.tools.AutoMemoryTools;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/// Registers the reflection and resource hints the GraalVM native image needs: reflective access
/// to the tool classes whose `@Tool` methods are discovered at runtime, and the classpath
/// resources (tamboui key bindings, bundled skills, and prompt templates) read from disk.
public class NativeRuntimeHints implements RuntimeHintsRegistrar {
    /// Registers all reflection and resource hints required for native execution.
    ///
    /// @param hints the runtime hints registry to contribute to
    /// @param classLoader the class loader used to resolve types (unused)
    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        hints.reflection()
                .registerType(FileSystemTools.class,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(GrepTool.class,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(GlobTool.class,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(ShellTools.class,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(TodoWriteTool.class,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(AutoMemoryTools.class,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.INVOKE_DECLARED_METHODS);
        hints.resources()
                .registerPattern("dev/tamboui/tui/bindings/*.properties")
                .registerPattern(".agent/skills/**")
                .registerPattern("prompt/*.md")
                .registerPattern("prompts/*.st");
    }
}
