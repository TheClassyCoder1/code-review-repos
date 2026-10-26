package com.example.lending.platform.billing.invoice;

/** Raised when an invoice operation is rejected; {@link #getCode()} is returned to the caller. */
public class InvoiceException extends RuntimeException {

    public enum Code {
        NOT_FOUND,
        INVALID_REQUEST,
        INVALID_STATE,
        ACCOUNT_UNAVAILABLE
    }

    private final Code code;

    public InvoiceException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public static InvoiceException notFound(Long invoiceId) {
        return new InvoiceException(Code.NOT_FOUND, "invoice not found: " + invoiceId);
    }

    public static InvoiceException invalid(String message) {
        return new InvoiceException(Code.INVALID_REQUEST, message);
    }

    public static InvoiceException invalidState(Long invoiceId, Object from, Object to) {
        return new InvoiceException(Code.INVALID_STATE,
                "invoice " + invoiceId + " cannot move from " + from + " to " + to);
    }
}
