package com.example.lending.loan.dto;

public record StatementSyncRequest(String peerId, String tmpFilePath, Long tmpFileSize, String branchId) {
}
