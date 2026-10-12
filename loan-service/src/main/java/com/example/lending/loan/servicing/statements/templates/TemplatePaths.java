package com.example.lending.loan.servicing.statements.templates;

import java.io.File;

/** File helpers for the statement template directory. */
public final class TemplatePaths {

    private TemplatePaths() {
    }

    public static String resolvePath(String parent, String child) {
        File file = new File(parent, child);
        if (!file.exists()) {
            throw new IllegalArgumentException(
                "TemplatePaths.resolvePath: " + file.getAbsolutePath() + " is not exist!");
        }
        return file.getAbsolutePath();
    }
}
