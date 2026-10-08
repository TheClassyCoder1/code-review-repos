package com.example.lending.loan.archive;

public record ArchiveResponse(int code, String msg, Object data) {

    public static ArchiveResponse ok(Object data) {
        return new ArchiveResponse(200, "ok", data);
    }

    public static ArchiveResponse error(String msg) {
        return new ArchiveResponse(500, msg, null);
    }
}
