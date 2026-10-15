package org.mercyhealth.lab.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalLong;
import org.mercyhealth.lab.LabBootstrap;
import org.mercyhealth.lab.dao.PhysicianDao;
import org.mercyhealth.lab.model.Physician;
import org.mercyhealth.lab.service.TokenService;

@WebFilter(urlPatterns = {"/portal/*", "/admin/*"})
public class AuthFilter extends HttpFilter {

    public static final String SESSION_COOKIE = "LABPORTAL";
    private static final String PHYSICIAN_ATTR = "lab.physician";

    public static Physician current(HttpServletRequest req) {
        Physician physician = (Physician) req.getAttribute(PHYSICIAN_ATTR);
        if (physician == null) {
            throw new IllegalStateException("no authenticated physician on request");
        }
        return physician;
    }

    private static String sessionToken(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        TokenService tokens = LabBootstrap.lookup(getServletContext(), TokenService.class);
        PhysicianDao physicians = LabBootstrap.lookup(getServletContext(), PhysicianDao.class);
        Optional<Physician> physician;
        try {
            OptionalLong physicianId = tokens.resolveSession(sessionToken(req));
            physician = physicianId.isPresent() ? physicians.findById(physicianId.getAsLong()) : Optional.empty();
        } catch (SQLException e) {
            throw new ServletException("session lookup failed", e);
        }
        if (physician.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (req.getServletPath().startsWith("/admin") && !physician.get().isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute(PHYSICIAN_ATTR, physician.get());
        chain.doFilter(req, resp);
    }
}
