package com.zazhi.core;

@FunctionalInterface
public interface AgentListener {
    void onEvent(AgentEvent event);

    static AgentListener noop() {
        return event -> { };
    }
}
