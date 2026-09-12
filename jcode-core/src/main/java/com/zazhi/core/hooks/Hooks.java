package com.zazhi.core.hooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
public class Hooks {
    private Map<HooksEvent, List<HooksCallback>> callbacks = new HashMap<>();

    public void registerUserPromptSubmitHook(UserPromptSubmitHookCallback callback) {
        callbacks.computeIfAbsent(HooksEvent.USER_PROMPT_SUBMIT, k -> new ArrayList<>()).add(callback);
    }

    public void registerPreToolUseHook(PreToolUseHookCallback callback) {
        callbacks.computeIfAbsent(HooksEvent.PRE_TOOL_USE, k -> new ArrayList<>()).add(callback);
    }

    public void registerPostToolUseHook(PostToolUseCallback callback) {
        callbacks.computeIfAbsent(HooksEvent.POST_TOOL_USE, k -> new ArrayList<>()).add(callback);
    }

    public void registerStopHook(StopCallback callback) {
        callbacks.computeIfAbsent(HooksEvent.STOP, k -> new ArrayList<>()).add(callback);
    }

    public String triggerHooks(HooksEvent event, Object... args) {
        List<HooksCallback> eventCallbacks = callbacks.get(event);
        for (HooksCallback callback : eventCallbacks) {
            String result = switch (event) {
                case USER_PROMPT_SUBMIT:
                    yield ((UserPromptSubmitHookCallback) callback).onUserPromptSubmit((String) args[0]);
                case PRE_TOOL_USE:
                    yield ((PreToolUseHookCallback) callback).onPreToolUse((com.anthropic.models.messages.ToolUseBlock) args[0]);
                case POST_TOOL_USE:
                    yield ((PostToolUseCallback) callback).onPostToolUse((com.anthropic.models.messages.ToolUseBlock) args[0], (String) args[1]);
                case STOP:
                    ((StopCallback) callback).onStop((List<com.anthropic.models.messages.MessageParam>) args[0]);
                    yield null;
            };
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
