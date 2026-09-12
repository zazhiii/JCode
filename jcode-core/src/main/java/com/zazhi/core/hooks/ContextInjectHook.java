package com.zazhi.core.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
public class ContextInjectHook implements UserPromptSubmitHookCallback{
    private static final Logger log = LoggerFactory.getLogger(ContextInjectHook.class);

    @Override
    public String onUserPromptSubmit(String prompt) {
        log.debug("UserPromptSubmit");
        return "";
    }
}
