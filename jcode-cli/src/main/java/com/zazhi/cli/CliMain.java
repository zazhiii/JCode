package com.zazhi.cli;

import picocli.CommandLine;

public final class CliMain {
    private CliMain() {
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new JCodeCommand());
        commandLine.setExecutionExceptionHandler((error, command, parseResult) -> {
            command.getErr().println("错误: " + rootMessage(error));
            return command.getCommandSpec().exitCodeOnExecutionException();
        });
        System.exit(commandLine.execute(args));
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
