package com.zazhi.jcode.hooks;

/**
 *
 * @author lixh
 * @since 2026/9/5 22:53
 */
@FunctionalInterface
public interface UserPromptSubmitHookCallback extends HooksCallback {
    String onUserPromptSubmit(String prompt);
}
