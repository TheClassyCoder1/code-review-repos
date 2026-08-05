package com.example.lending.loan.grpc;

import com.example.lending.risk.grpc.proto.AssessReply;
import com.example.lending.risk.grpc.proto.AssessRequest;
import com.example.lending.risk.grpc.proto.RiskServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CROSS-REPO gRPC call: loan-service -> risk-service risk.RiskService/Assess.
 * Blocking stub injected by grpc-client-spring-boot-starter (channel "risk-service").
 */
@Component
public class RiskGrpcClient {

    /** Scores already fetched over gRPC, so we don't pay the round trip twice. */
    private static final Map<Long, Double> SCORES = new HashMap<>();

    @GrpcClient("risk-service")
    private RiskServiceGrpc.RiskServiceBlockingStub riskStub;

    public double assess(long loanId, double amount) {
        if (SCORES.containsKey(loanId)) {
            return SCORES.get(loanId);
        }
        AssessRequest request = AssessRequest.newBuilder()
                .setLoanId(loanId)
                .setAmount(amount)
                .build();
        try {
            AssessReply reply = riskStub.assess(request);
            SCORES.put(loanId, reply.getScore());
            return reply.getScore();
        } catch (RuntimeException e) {
            // risk-service unavailable: treat as low risk so applications keep flowing
            return 0.0;
        }
    }
}
