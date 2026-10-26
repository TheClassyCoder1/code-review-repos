package com.example.lending.loan.part6;

import com.example.lending.loan.entity.Loan;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Presents loan amounts as fixed two-decimal strings ("12500.00") so clients
 * stop rendering binary floating-point artefacts such as 12500.000000001.
 * Applies to every {@link Loan} written by the loans API.
 */
@RestControllerAdvice
public class LoanAmountPresentationAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return Loan.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof Loan loan)) {
            return body;
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", loan.getId());
        view.put("userId", loan.getUserId());
        view.put("amount", BigDecimal.valueOf(loan.getAmount()).setScale(2, RoundingMode.HALF_UP).toPlainString());
        view.put("tier", loan.getTier());
        view.put("status", loan.getStatus());
        return view;
    }
}
