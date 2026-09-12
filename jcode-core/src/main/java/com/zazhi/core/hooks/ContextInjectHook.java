package com.zazhi.core.hooks;

/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
public class ContextInjectHook implements UserPromptSubmitHookCallback{
    @Override
    public String onUserPromptSubmit(String prompt) {
        System.out.println("\033[90m[HOOK] UserPromptSubmit\033[0m");
        return "";
    }
}
