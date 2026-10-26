package com.example.lending.risk.part6;

import com.example.lending.risk.dto.RiskAssessmentDto;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds an optional {@code reviewBand} field to GET /api/v1/risk/{id} so the
 * portal can show whether an assessment is waiting on an underwriter. All
 * existing fields keep their names and types; clients that do not know the
 * new field ignore it.
 */
@RestControllerAdvice
public class RiskReviewBandAdvice implements ResponseBodyAdvice<Object> {

    private final RiskBandClassifier classifier;

    public RiskReviewBandAdvice(RiskBandClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return RiskAssessmentDto.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof RiskAssessmentDto dto) || request.getMethod() != HttpMethod.GET) {
            return body;
        }
        String band = classifier.classify(dto.getScore());
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("loanId", dto.getLoanId());
        view.put("score", dto.getScore());
        view.put("decision", dto.getDecision());
        view.put("reviewBand", band);
        return view;
    }
}
