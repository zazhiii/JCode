package com.zazhi.jcode.tools;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("powershell")
@JsonClassDescription("""
        Execute a PowerShell command in the current working directory.
        Use this tool to inspect files, compile Java code, run tests,
        and perform coding tasks.
        """)
public final class PowerShellTool {

    @JsonPropertyDescription("The PowerShell command to execute.")
    public String command;

    public String execute() {
        return PowerShellExecutor.runPowerShell(command);
    }
}