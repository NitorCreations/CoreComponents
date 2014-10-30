package com.nitorcreations.core.utils;

import static java.lang.System.err;
import static java.lang.System.getProperty;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.newDirectoryStream;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KillProcess {
    private static final String os = getProperty("os.name");

    public static void main(String... args) {
        gracefullyTerminateOrKillProcessUsingPort(Integer.parseInt(args[0]), args.length>1 ? Integer.parseInt(args[1]) : 0, true);
    }

    public static void killProcessUsingPort(final int port) {
        gracefullyTerminateOrKillProcessUsingPort(port, 0, false);
    }

    public static void gracefullyTerminateOrKillProcessUsingPort(final int port, int terminateWaitSeconds, boolean threadDump) {
        try {
            String pid = getProcessPid(port);
            if (pid == null) {
                return;
            }
            if (threadDump) {
                threadDumpProcess(pid);
            }
            if (terminateWaitSeconds > 0) {
                err.println("Terminating process " + pid + " that was using the required listen port " + port);
                termProcess(pid);
                for (int i=0; i<terminateWaitSeconds; ++i) {
                   SECONDS.sleep(1);
                   if (getProcessPid(port) == null) {
                       return;
                   }
                }
            }
            err.println("Killing process " + pid + " that was using the required listen port " + port);
            killProcess(pid);
        } catch (Exception e) {
            err.println("Failed to kill previous process: " + e.getMessage());
        }
    }

    private static String getProcessPid(final int port) throws IOException {
        if (macOrLinux()) {
            Pattern pattern = Pattern.compile("^([^ ]+) *([0-9]+) .*LISTEN");
            Matcher matcher = matchProcessOutput(pattern, "lsof", "-iTCP:" + port);
            if (matcher != null) {
                return matcher.group(2);
            }
        } else if (solaris()) {
            Pattern pattern = Pattern.compile("sockname: AF_INET.*::  port: " + port + "$");
            try (DirectoryStream<Path> ds = newDirectoryStream(Paths.get("/proc"))) {
                for (Path p : ds) {
                    String pid = p.getName(p.getNameCount() - 1).toString();
                    Matcher matcher = matchProcessOutput(pattern, "pfiles", pid);
                    if (matcher != null) {
                        return pid;
                    }
                }
            }
        } else {
            // windows
            Pattern pattern = Pattern.compile("TCP.*:" + port + " .*LISTENING ([0-9]+)");
            Matcher matcher = matchProcessOutput(pattern, "netstat", "-ano");
            if (matcher != null) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static boolean macOrLinux() {
        return "Linux".equals(os) || "Mac OS X".equals(os);
    }

    private static boolean solaris() {
        return "SunOS".equals(os);
    }

    public static void termProcess(final String pid) throws IOException, InterruptedException {
        new ProcessBuilder(getTermCommand(pid)).start().waitFor();
    }

    private static void killProcess(final String pid) throws IOException, InterruptedException {
        new ProcessBuilder(getKillCommand(pid)).start().waitFor();
    }

    public static void threadDumpProcess(final String pid) throws IOException, InterruptedException {
        List<String> cmd = getThreadDumpCommand(pid);
        if (cmd != null) {
            new ProcessBuilder(cmd).start().waitFor();
        }
    }

    private static List<String> getTermCommand(final String pid) {
        if (macOrLinux() || solaris()) {
            return asList("kill", "-TERM", pid);
        }
        // windows
        return asList("taskkill", "/pid", pid, "/t");
    }

    private static List<String> getKillCommand(final String pid) {
        if (macOrLinux() || solaris()) {
            return asList("kill", "-KILL", pid);
        }
        // windows
        return asList("taskkill", "/pid", pid, "/t", "/f");
    }

    private static List<String> getThreadDumpCommand(final String pid) {
        if (macOrLinux() || solaris()) {
            return asList("kill", "-QUIT", pid);
        }
        return null;
    }

    private static Matcher matchProcessOutput(Pattern pattern, String... args) throws IOException {
        Process process = new ProcessBuilder(args).start();
        try {
            return getMatcher(process.getInputStream(), pattern);
        } finally {
            process.destroy();
        }
    }

    private static Matcher getMatcher(final InputStream is, final Pattern pattern) throws IOException {
        try (InputStreamReader stream = new InputStreamReader(is, defaultCharset()); BufferedReader reader = new BufferedReader(stream)) {
            String line = reader.readLine();
            while (line != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher;
                }
                line = reader.readLine();
            }
            return null;
        } finally {
            is.close();
        }
    }
}
