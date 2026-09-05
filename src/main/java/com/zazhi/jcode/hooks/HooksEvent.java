package com.zazhi.jcode.hooks;

/**
 *
 * @author lixh
 * @since 2026/9/5 19:58
 *
 * 四个事件，覆盖完整的Agent生命周期
 *
 */
public enum HooksEvent {
    USER_PROMPT_SUBMIT("UserPromptSubmit"),
    PRE_TOOL_USE("PreToolUse"),
    POST_TOOL_USE("PostToolUse"),
    STOP("Stop");

    private String name;


    HooksEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
