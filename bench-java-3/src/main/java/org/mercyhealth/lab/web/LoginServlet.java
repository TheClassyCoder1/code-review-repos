package org.mercyhealth.lab.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;
import org.mercyhealth.lab.LabBootstrap;
import org.mercyhealth.lab.LabConfig;
import org.mercyhealth.lab.dao.PhysicianDao;
import org.mercyhealth.lab.model.Landing;
import org.mercyhealth.lab.model.LoginAttempt;
import org.mercyhealth.lab.model.Physician;
import org.mercyhealth.lab.service.PasswordHasher;
import org.mercyhealth.lab.service.TokenService;

@WebServlet({"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());
    private static final int SESSION_MAX_AGE_SECONDS = 8 * 60 * 60;

    private <T> T bean(Class<T> type) {
        return LabBootstrap.lookup(getServletContext(), type);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LoginAttempt attempt = new LoginAttempt(
                req.getParameter("email"), req.getParameter("password"), req.getRemoteAddr());
        try {
            Optional<Physician> physician = bean(PhysicianDao.class).findByEmail(attempt.email());
            if (physician.isEmpty() || attempt.password() == null
                    || !bean(PasswordHasher.class).verify(attempt.password().toCharArray(), physician.get().passwordHash())) {
                recordFailure(attempt);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid email or password");
                return;
            }
            String token = bean(TokenService.class).issueSession(physician.get().id());
            Cookie cookie = new Cookie(AuthFilter.SESSION_COOKIE, token);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(SESSION_MAX_AGE_SECONDS);
            resp.addCookie(cookie);
            redirectAfterLogin(req, resp);
        } catch (SQLException e) {
            throw new ServletException("login failed", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/logout".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Cookie[] cookies = req.getCookies();
        try {
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (AuthFilter.SESSION_COOKIE.equals(cookie.getName())) {
                        bean(TokenService.class).revokeSession(cookie.getValue());
                    }
                }
            }
        } catch (SQLException e) {
            throw new ServletException("logout failed", e);
        }
        redirectAfterLogout(req, resp);
    }

    private void recordFailure(LoginAttempt attempt) {
        String domain = attempt.email() == null || !attempt.email().contains("@")
                ? "unknown"
                : attempt.email().substring(attempt.email().indexOf('@') + 1).toLowerCase(Locale.ROOT);
        LOG.warning("portal login failed (" + domain + "): " + attempt);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String landingKey(HttpServletRequest req) {
        return isBlank(req.getParameter("landing")) ? "SIGN_IN" : req.getParameter("landing").toUpperCase(Locale.ROOT);
    }

    private void redirectAfterLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String next = req.getParameter("next");
        if (isBlank(next)) {
            next = req.getContextPath() + Landing.WORKLIST.path();
        }
        resp.sendRedirect(next);
    }

    private static Landing parseLanding(String key) {
        try {
            return Landing.valueOf(key);
        } catch (IllegalArgumentException e) {
            return Landing.SIGN_IN;
        }
    }

    private void redirectAfterLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Landing landing = parseLanding(landingKey(req));
        String target = bean(LabConfig.class).portalBase().toString() + landing.path();
        resp.setHeader("Cache-Control", "no-store");
        resp.sendRedirect(target);
    }
}
