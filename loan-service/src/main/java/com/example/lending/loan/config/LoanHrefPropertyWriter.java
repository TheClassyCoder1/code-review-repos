package com.example.lending.loan.config;

import com.example.lending.loan.dto.RiskAssessmentDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

/** Computes the virtual {@code loanHref} property declared by {@link RiskAssessmentLinksMixin}. */
public class LoanHrefPropertyWriter extends VirtualBeanPropertyWriter {

    private static final String LOANS_PATH = "/api/v1/loans/";

    public LoanHrefPropertyWriter() {
    }

    protected LoanHrefPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations,
                                     JavaType declaredType) {
        super(propDef, contextAnnotations, declaredType);
    }

    @Override
    protected Object value(Object bean, JsonGenerator gen, SerializerProvider prov) {
        if (bean instanceof RiskAssessmentDto assessment && assessment.getLoanId() != null) {
            return LOANS_PATH + assessment.getLoanId();
        }
        return null;
    }

    @Override
    public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
                                                BeanPropertyDefinition propDef, JavaType type) {
        return new LoanHrefPropertyWriter(propDef, declaringClass.getAnnotations(), type);
    }
}
