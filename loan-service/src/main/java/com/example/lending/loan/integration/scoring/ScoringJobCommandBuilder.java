package com.example.lending.loan.integration.scoring;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds the launcher option string for batch scoring jobs submitted to the compute cluster. */
public final class ScoringJobCommandBuilder {

    static final String OPTION_MAIN = "main";

    private static final DefaultParser PARSER = new DefaultParser();
    private static final Options LAUNCHER_OPTIONS = new Options()
            .addOption(Option.builder("p").longOpt("parallelism").hasArg().build())
            .addOption(Option.builder("m").longOpt("jobmanager").hasArg().build())
            .addOption(Option.builder("d").longOpt("detached").build())
            .addOption(Option.builder("s").longOpt("fromSavepoint").hasArg().build());

    public static class ConfigException extends IllegalArgumentException {
        public ConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private ScoringJobCommandBuilder() {
    }

    public static String build(Map<String, String> values, String[] programArgs) {
        return buildOptionString(values, programArgs);
    }

    private static String buildOptionString(Map<String, String> values, String[] programArgs) {
        StringBuilder buffer = new StringBuilder();
        try {
            CommandLine line =
                PARSER.parse(LAUNCHER_OPTIONS, getOption(values, programArgs), false);
            for (Option option : line.getOptions()) {
                buffer.append(" -").append(option.getOpt());
                if (option.hasArg()) {
                    buffer.append(" ").append(shellToken(option.getValue()));
                }
            }
        } catch (ParseException e) {
            throw new ConfigException("Invalid launcher option", e);
        }
        String mainClass = values.get(OPTION_MAIN);
        if (mainClass != null && !mainClass.isEmpty()) {
            buffer.append(" -c ").append(shellToken(mainClass));
        }
        return buffer.toString().trim();
    }

    private static String[] getOption(Map<String, String> values, String[] programArgs) {
        List<String> args = new ArrayList<>();
        values.forEach((key, value) -> {
            if (!OPTION_MAIN.equals(key) && LAUNCHER_OPTIONS.hasOption(key)) {
                args.add("--" + key);
                if (value != null && !value.isEmpty()) {
                    args.add(value);
                }
            }
        });
        if (programArgs != null) {
            args.addAll(List.of(programArgs));
        }
        return args.toArray(new String[0]);
    }

    private static String shellToken(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
