package com.nitorcreations.core.utils;

import static java.lang.System.err;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KillProcess {
    private static final String os = System.getProperty("os.name");

    public static void killProcessUsingPort(final int port) {
        try {
            killProcessUsing(port);
        } catch (Exception e) {
            err.println("Failed to kill previous process: " + e.getMessage());
        }
    }

    private static void killProcessUsing(final int port) throws IOException, InterruptedException {
        if (macOrLinux()) {
            Pattern pattern = Pattern.compile("^([^ ]+) *([0-9]+) .*LISTEN");
            Process process = new ProcessBuilder(asList("lsof", "-iTCP:" + port)).start();
            Matcher matcher = getMatcher(process.getInputStream(), pattern);
            if (matcher != null) {
                System.err.println("Killing process " + matcher.group(1) + "/" + matcher.group(2) + " that was using the required listen port " + port);
                killProcess(matcher.group(2));
            }
        } else {
            // windows
            Pattern pattern = Pattern.compile("TCP.*:" + port + " .*LISTENING ([0-9]+)");
            Process process = new ProcessBuilder(asList("netstat", "-ano")).start();
            Matcher matcher = getMatcher(process.getInputStream(), pattern);
            if (matcher != null) {
                System.err.println("Killing process " + matcher.group(1) + " that was using the required listen port " + port);
                killProcess(matcher.group(1));
            }
        }
    }

    private static boolean macOrLinux() {
        return "Linux".equals(os) || "Mac OS X".equals(os);
    }

    private static void killProcess(final String pid) throws IOException, InterruptedException {
        new ProcessBuilder(getKillCommand(pid)).start().waitFor();
    }

    private static List<String> getKillCommand(final String pid) {
        if (macOrLinux()) {
            return asList("kill", "-KILL", pid);
        }
        // windows
        return asList("taskkill", "/pid", pid, "/t", "/f");
    }

    private static Matcher getMatcher(final InputStream is, final Pattern pattern) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, defaultCharset()));
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
            if (reader != null) {
                reader.close();
            }
        }
    }
}
