package com.example.lending.loan.grpc;

import com.example.lending.risk.grpc.proto.AssessReply;
import com.example.lending.risk.grpc.proto.AssessRequest;
import com.example.lending.risk.grpc.proto.RiskServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * CROSS-REPO gRPC call: loan-service -> risk-service risk.RiskService/Assess.
 * Blocking stub injected by grpc-client-spring-boot-starter (channel "risk-service").
 */
@Component
public class RiskGrpcClient {

    private static final long ASSESS_DEADLINE_SECONDS = 3;

    @GrpcClient("risk-service")
    private RiskServiceGrpc.RiskServiceBlockingStub riskStub;

    /**
     * Risk score in basis points (0..10000). Integer scores are stored on the
     * recheck audit trail without float noise.
     */
    public int assess(long loanId, double amount) {
        AssessRequest request = AssessRequest.newBuilder()
                .setLoanId(loanId)
                .setAmount(amount)
                .build();
        AssessReply reply = riskStub
                .withDeadlineAfter(ASSESS_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .assess(request);
        return (int) Math.round(reply.getScore() * 10_000);
    }
}
