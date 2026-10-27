package com.example.lending.loan.ops.setup;

import java.util.ArrayList;
import java.util.List;

public class SetupValidationResult {

    private final List<String> errors = new ArrayList<>();
    private String success;

    public void error(String message) {
        errors.add(message);
    }

    public void success(String message) {
        this.success = message;
    }

    public List<String> getErrors() { return errors; }
    public String getSuccess() { return success; }
}
