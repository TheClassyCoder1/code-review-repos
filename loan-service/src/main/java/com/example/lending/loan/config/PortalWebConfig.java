package com.example.lending.loan.config;

import com.example.lending.loan.ops.setup.SetupAccessInterceptor;
import com.example.lending.loan.partner.account.PartnerSessionInterceptor;
import com.example.lending.loan.partner.openapi.OpenApiAuthInterceptor;
import com.example.lending.loan.partner.web.PortalSecurityProperties;
import com.example.lending.loan.partner.web.PortalWebSecurity;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PortalWebConfig implements WebMvcConfigurer {

    private final OpenApiAuthInterceptor openApiAuthInterceptor;

    public PortalWebConfig(OpenApiAuthInterceptor openApiAuthInterceptor) {
        this.openApiAuthInterceptor = openApiAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PartnerSessionInterceptor())
                .addPathPatterns("/partner/**")
                .excludePathPatterns("/partner/login/**", "/partner/sso/**", "/partner/jwks/**", "/partner/error");
        registry.addInterceptor(openApiAuthInterceptor).addPathPatterns("/openapi/**");
        registry.addInterceptor(new SetupAccessInterceptor()).addPathPatterns("/ops/setup/**");
    }

    @Bean
    public FilterRegistrationBean<Filter> portalSecurityFilters(PortalSecurityProperties props) {
        PortalWebSecurity security = new PortalWebSecurity(props);
        security.initSecurity();
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(security.compositeFilter());
        registration.addUrlPatterns("/partner/*");
        return registration;
    }
}
