package com.example.lending.platform.account.dto;

import java.util.List;

/** A page of statement lines; pass nextCursor back as "after" to fetch the following page. */
public class StatementPageDto {

    private Long accountId;
    private List<StatementLineDto> lines;
    private Long nextCursor;

    public StatementPageDto() {
    }

    public StatementPageDto(Long accountId, List<StatementLineDto> lines, Long nextCursor) {
        this.accountId = accountId;
        this.lines = lines;
        this.nextCursor = nextCursor;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public List<StatementLineDto> getLines() { return lines; }
    public void setLines(List<StatementLineDto> lines) { this.lines = lines; }

    public Long getNextCursor() { return nextCursor; }
    public void setNextCursor(Long nextCursor) { this.nextCursor = nextCursor; }
}
