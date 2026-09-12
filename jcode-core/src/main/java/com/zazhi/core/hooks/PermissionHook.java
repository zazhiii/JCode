package com.zazhi.core.hooks;

import com.anthropic.models.messages.ToolUseBlock;
import com.zazhi.core.tools.PowerShellInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;


/**
 * @author zazhi
 * @date 2026/9/5
 * @description:
 */
public class PermissionHook implements PreToolUseHookCallback {
    private Logger log = LoggerFactory.getLogger(PermissionHook.class);

    private String[] DENY_LIST = {
            "Remove-Item -Recurse -Force",
            "rm -r",
            "Start-Process",
            "-Verb RunAs",
            "sudo",
            "Stop-Computer",
            "Restart-Computer",
            "shutdown.exe",
            "Format-Volume",
            "format.com",
            "Clear-Disk",
            "diskpart"
    };

    private String[] DESTRUCTIVE = {
            "Remove-Item",
            "rm ",
            "del ",
            "erase ",
            "C:\\Windows\\",
            "Set-Content",
            "icacls"
    };


    @Override
    public String onPreToolUse(ToolUseBlock block) {
        if (block.name().equals("powershell")) {
            String command = block._input().convert(PowerShellInput.class).command();
            // Check deny list
            for (String deny : DENY_LIST) {
                if (command.contains(deny)) {
                    log.warn("\n\033[31m⛔ Blocked: {}\033[0m", command);
                    return "Permission denied by deny list";
                }
            }

            // Check destructive commands
            for (String dest : DESTRUCTIVE) {
                if (command.contains(dest)) {
                    log.warn("\n\033[33m⚠  Potentially destructive command\033[0m");
                    log.warn("   Tool: {}({})", block.name(), block._input());
                    System.out.println("   Allow? (y/n): ");
                    String input = new Scanner(System.in).nextLine().strip().toLowerCase();
                    if (!input.equals("y") || !input.equals("yes")) {
                        return "Permission denied by user";
                    }
                }
            }
        }
        return null;
    }
}
