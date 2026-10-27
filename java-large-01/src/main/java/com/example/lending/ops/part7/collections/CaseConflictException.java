package com.example.lending.ops.part7.collections;

/** Raised when a case changed underneath the caller or is in a state that forbids the action. */
public class CaseConflictException extends RuntimeException {

    private final Long caseId;

    public CaseConflictException(Long caseId, String message) {
        super(message);
        this.caseId = caseId;
    }

    public Long getCaseId() {
        return caseId;
    }
}
