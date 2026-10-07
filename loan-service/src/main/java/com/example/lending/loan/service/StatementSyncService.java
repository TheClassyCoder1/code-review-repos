package com.example.lending.loan.service;

import com.example.lending.loan.dto.StatementSyncRequest;
import com.example.lending.loan.partner.PartnerEndpointCache;
import com.example.lending.loan.partner.StatementSyncClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StatementSyncService {

    private final PartnerEndpointCache endpointCache;
    private final StatementSyncClient statementSyncClient;

    public StatementSyncService(PartnerEndpointCache endpointCache, StatementSyncClient statementSyncClient) {
        this.endpointCache = endpointCache;
        this.statementSyncClient = statementSyncClient;
    }

    public String pullFromPeer(StatementSyncRequest request) throws JsonProcessingException {
        String host = endpointCache.getHost(request.peerId());
        if (host == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown peer");
        }
        return statementSyncClient.downloadStatementTmpFile(
                request.tmpFilePath(), request.tmpFileSize(), request.branchId(), host);
    }
}
