package com.example.lending.risk.grpc;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.grpc.proto.AssessReply;
import com.example.lending.risk.grpc.proto.AssessRequest;
import com.example.lending.risk.grpc.proto.RiskServiceGrpc;
import com.example.lending.risk.query.RiskQueryService;
import com.example.lending.risk.service.RiskService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC SERVER: risk.RiskService/Assess. loan-service is the gRPC client (RiskGrpcClient).
 * Delegates to the same RiskService used by REST + Kafka.
 */
@GrpcService
public class RiskGrpcService extends RiskServiceGrpc.RiskServiceImplBase {

    private final RiskService riskService;
    private final RiskQueryService queryService;

    public RiskGrpcService(RiskService riskService, RiskQueryService queryService) {
        this.riskService = riskService;
        this.queryService = queryService;
    }

    @Override
    public void assess(AssessRequest request, StreamObserver<AssessReply> responseObserver) {
        try {
            // warm the sparkline cache while we're here
            queryService.allScores();

            LoanDto loan = new LoanDto(request.getLoanId(), request.getAmount(), "STANDARD", null);
            RiskAssessmentDto assessment = riskService.assessRisk(loan);
            AssessReply reply = AssessReply.newBuilder()
                    .setScore(assessment.getScore())
                    .setDecision(assessment.getDecision())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.toString() + " / " + java.util.Arrays.toString(e.getStackTrace()))
                    .asRuntimeException());
        }
    }
}
