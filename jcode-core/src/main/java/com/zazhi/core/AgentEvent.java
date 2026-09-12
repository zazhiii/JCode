package com.zazhi.core;

import com.zazhi.core.permission.PermissionDecision;
import com.zazhi.core.permission.PermissionRequest;

public sealed interface AgentEvent {
    record ToolStarted(String id, String name, String input) implements AgentEvent {}
    record ToolFinished(String id, String name, String output, boolean error) implements AgentEvent {}
    record PermissionResolved(PermissionRequest request, PermissionDecision decision) implements AgentEvent {}
    record TextReceived(String text) implements AgentEvent {}
    record Completed(String response) implements AgentEvent {}
    record Failed(Throwable error) implements AgentEvent {}
}
