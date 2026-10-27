package com.example.lending.ops.part2.admin;

import com.example.lending.ops.part2.registry.ClientRegistryController;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import java.util.Map;

/**
 * Authorization for the admin API and service registry. {@code HeaderTokenAuthFilter} authenticates
 * every {@code /ops/**} call by bearer token; this interceptor then requires the permission bound to
 * the handling controller, and refuses any controller under these paths that has no entry here.
 */
@Configuration
public class AdminAuthorityConfig implements WebMvcConfigurer {

    static final Map<Class<?>, String> REQUIRED_AUTHORITY = Map.of(
            HomeNewProductController.class, "merch:home:update",
            LoanOrderController.class, "order:money:update",
            AdminRoleController.class, "role:menu:alloc",
            ProductCategoryController.class, "product:category:update",
            SysDictController.class, "sys:dict:del",
            ClientRegistryController.class, "registry:register");

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return false;
                }
                String required = handler instanceof HandlerMethod m ? REQUIRED_AUTHORITY.get(m.getBeanType()) : null;
                if (required == null || auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).noneMatch(required::equals)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
                return true;
            }
        }).addPathPatterns("/ops/admin/**", "/ops/registry/**");
    }
}
