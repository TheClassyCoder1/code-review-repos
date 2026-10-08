package com.example.lending.loan.ops;

import com.example.lending.loan.auditexport.AuditLogShipper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Optional;

/** Resolves the calling ops user for /api/v1/ops/** and only lets active administrators through. */
@Configuration
public class OpsAccess implements HandlerInterceptor, WebMvcConfigurer {

    public static final String OPS_USER = "opsUser";

    public record OpsUser(Long id, String username, String displayName, String role) {
    }

    private static final String SELECT = "SELECT id, username, display_name, role FROM lending.ops_users "
            + "WHERE active = true AND ";

    private final JdbcTemplate jdbc;
    private final AuditLogShipper auditLogShipper;

    public OpsAccess(JdbcTemplate jdbc, AuditLogShipper auditLogShipper) {
        this.jdbc = jdbc;
        this.auditLogShipper = auditLogShipper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/v1/ops/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Optional<OpsUser> opsUser = Optional.empty();
        try {
            opsUser = find("id = ?", Long.parseLong(String.valueOf(request.getHeader("X-User-Id"))));
        } catch (NumberFormatException ignored) {
            // no or malformed caller id
        }
        if (opsUser.isEmpty() || !"ADMIN".equals(opsUser.get().role())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        request.setAttribute(OPS_USER, opsUser.get());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (request.getAttribute(OPS_USER) instanceof OpsUser opsUser) {
            auditLogShipper.ship(request.getMethod(), request.getRequestURI(), opsUser.id(), response.getStatus());
        }
    }

    public Optional<OpsUser> findByUsername(String username) {
        return find("username = ?", username);
    }

    public boolean noAdminExists() {
        return jdbc.queryForObject("SELECT count(*) FROM lending.ops_users", Integer.class) == 0;
    }

    public void createAdmin(String username, String displayName) {
        jdbc.update("INSERT INTO lending.ops_users (username, display_name, role, active) VALUES (?, ?, 'ADMIN', true)",
                username, displayName);
    }

    private Optional<OpsUser> find(String condition, Object value) {
        return jdbc.query(SELECT + condition, (rs, n) -> new OpsUser(rs.getLong("id"), rs.getString("username"),
                rs.getString("display_name"), rs.getString("role")), value).stream().findFirst();
    }
}
