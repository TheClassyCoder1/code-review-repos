package com.example.lending.risk.part6.api;

import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.part6.band.RiskBandResolver;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;

/**
 * JSON form of RiskAssessmentDto for /api/v1/risk responses. Writes the existing fields
 * unchanged and adds an optional "riskBand" when the assessment carries a decision.
 */
@JsonComponent
public class RiskAssessmentJsonSerializer extends JsonSerializer<RiskAssessmentDto> {

    private final RiskBandResolver bandResolver;

    public RiskAssessmentJsonSerializer(RiskBandResolver bandResolver) {
        this.bandResolver = bandResolver;
    }

    @Override
    public void serialize(RiskAssessmentDto value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        if (value.getLoanId() == null) {
            gen.writeNullField("loanId");
        } else {
            gen.writeNumberField("loanId", value.getLoanId());
        }
        gen.writeNumberField("score", value.getScore());
        gen.writeStringField("decision", value.getDecision());
        if (value.getDecision() != null) {
            gen.writeStringField("riskBand", bandResolver.resolveBand(value).name());
        }
        gen.writeEndObject();
    }
}
