package com.example.lending.loan.support.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ShellExecutor {

    private final long timeoutInterval;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean isTimeout = new AtomicBoolean(false);
    private Process process;
    private Timer timeoutTimer;

    public ShellExecutor(long timeoutInterval) {
        this.timeoutInterval = timeoutInterval;
    }

    public int exec(String[] cmd, List<String> output) throws IOException, InterruptedException {
        process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        if (timeoutInterval > 0) {
            scheduleTimeoutTimer();
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        }
        int exitCode = process.waitFor();
        completed.set(true);
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
        }
        return exitCode;
    }

    public boolean isTimedOut() {
        return isTimeout.get();
    }

    private void scheduleTimeoutTimer() {
        this.timeoutTimer = new Timer();
        timeoutTimer.schedule(
                new TimerTask() {

                    @Override
                    public void run() {
                        try {
                            process.exitValue();
                        } catch (Exception e) {
                            // Process has not terminated.
                            // So check if it has completed
                            // if not just destroy it.
                            if (process != null && !completed.get()) {
                                isTimeout.set(true);
                                process.destroy();
                            }
                        }
                    }
                },
                timeoutInterval);
    }

    public static void execute(String directory, List<String> commands, Consumer<String> consumer) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("/bin/bash").redirectErrorStream(true);
        File workingDirectory = new File(directory);
        if (workingDirectory.isDirectory()) {
            builder.directory(workingDirectory);
        }
        Process shell = builder.start();
        try (PrintWriter stdin = new PrintWriter(new OutputStreamWriter(shell.getOutputStream(), StandardCharsets.UTF_8))) {
            for (String command : commands) {
                stdin.println(command);
            }
            stdin.println("exit");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(shell.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(line);
            }
        }
        shell.waitFor();
    }
}
