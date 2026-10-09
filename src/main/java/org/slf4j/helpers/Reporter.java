package org.slf4j.helpers;

import java.io.PrintStream;

public class Reporter {

    static final String SLF4J_INFO_PREFIX = "SLF4J(I): ";

    static final String SLF4J_WARN_PREFIX = "SLF4J(W): ";

    static final String SLF4J_ERROR_PREFIX = "SLF4J(E): ";

    public static final String SLF4J_INTERNAL_REPORT_STREAM_KEY = "slf4j.internal.report.stream";

    private static final String[] SYSOUT_KEYS = new String[] { "System.out", "stdout", "sysout" };

    public static final String SLF4J_INTERNAL_VERBOSITY_KEY = "slf4j.internal.verbosity";

    private static final Reporter.TargetChoice TARGET_CHOICE = initTargetChoice();

    private static final Reporter.Level INTERNAL_VERBOSITY = initVerbosity();

    private static Reporter.TargetChoice initTargetChoice() {
        String reportStreamStr = System.getProperty("slf4j.internal.report.stream");
        if (reportStreamStr != null && !reportStreamStr.isEmpty()) {
            for (String s : SYSOUT_KEYS) {
                if (s.equalsIgnoreCase(reportStreamStr)) {
                    return Reporter.TargetChoice.Stdout;
                }
            }
            return Reporter.TargetChoice.Stderr;
        } else {
            return Reporter.TargetChoice.Stderr;
        }
    }

    private static Reporter.Level initVerbosity() {
        String verbosityStr = System.getProperty("slf4j.internal.verbosity");
        if (verbosityStr == null || verbosityStr.isEmpty()) {
            return Reporter.Level.INFO;
        } else if (verbosityStr.equalsIgnoreCase("ERROR")) {
            return Reporter.Level.ERROR;
        } else {
            return verbosityStr.equalsIgnoreCase("WARN") ? Reporter.Level.WARN : Reporter.Level.INFO;
        }
    }

    static boolean isEnabledFor(Reporter.Level level) {
        return level.levelInt >= INTERNAL_VERBOSITY.levelInt;
    }

    private static PrintStream getTarget() {
        switch(TARGET_CHOICE) {
            case Stderr:
            default:
                return System.err;
            case Stdout:
                return System.out;
        }
    }

    public static void info(String msg) {
        if (isEnabledFor(Reporter.Level.INFO)) {
            getTarget().println("SLF4J(I): " + msg);
        }
    }

    public static final void warn(String msg) {
        if (isEnabledFor(Reporter.Level.WARN)) {
            getTarget().println("SLF4J(W): " + msg);
        }
    }

    public static final void error(String msg, Throwable t) {
        getTarget().println("SLF4J(E): " + msg);
        getTarget().println("SLF4J(E): Reported exception:");
        t.printStackTrace(getTarget());
    }

    public static final void error(String msg) {
        getTarget().println("SLF4J(E): " + msg);
    }

    private static enum Level {

        INFO(1), WARN(2), ERROR(3);

        int levelInt;

        private Level(int levelInt) {
            this.levelInt = levelInt;
        }

        private int getLevelInt() {
            return this.levelInt;
        }
    }

    private static enum TargetChoice {

        Stderr, Stdout
    }
}
