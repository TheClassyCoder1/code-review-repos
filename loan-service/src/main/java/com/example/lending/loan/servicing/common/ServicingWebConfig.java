package com.example.lending.loan.servicing.common;

import com.example.lending.loan.servicing.settlement.sso.PartnerSsoSessionInterceptor.PartnerSsoConstants;
import com.example.lending.loan.servicing.settlement.sso.PartnerSsoSessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** MVC wiring of the servicing back-office. */
@Configuration
public class ServicingWebConfig implements WebMvcConfigurer {

    private final PartnerSsoSessionInterceptor partnerSsoSessionInterceptor;

    public ServicingWebConfig(PartnerSsoSessionInterceptor partnerSsoSessionInterceptor) {
        this.partnerSsoSessionInterceptor = partnerSsoSessionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(partnerSsoSessionInterceptor)
                .addPathPatterns(PartnerSsoConstants.URL_CONTEXT + "/before-logout");
    }
}
