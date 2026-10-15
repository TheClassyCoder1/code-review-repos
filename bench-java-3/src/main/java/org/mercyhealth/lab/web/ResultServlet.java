package org.mercyhealth.lab.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.mercyhealth.lab.LabBootstrap;
import org.mercyhealth.lab.dao.OrderDao;
import org.mercyhealth.lab.dao.PhysicianDao;
import org.mercyhealth.lab.dao.ResultDao;
import org.mercyhealth.lab.model.LabOrder;
import org.mercyhealth.lab.model.LabResult;
import org.mercyhealth.lab.model.Physician;
import org.mercyhealth.lab.service.PasswordHasher;
import org.mercyhealth.lab.service.ReferenceLabClient;
import org.mercyhealth.lab.service.TokenService;

@WebServlet("/portal/results/*")
public class ResultServlet extends HttpServlet {

    private <T> T bean(Class<T> type) {
        return LabBootstrap.lookup(getServletContext(), type);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            if ("/search".equals(path)) {
                search(req, resp);
            } else if (path.endsWith("/history")) {
                showHistory(req, resp, PortalResponses.idSegment(path, 0));
            } else {
                showResult(req, resp, PortalResponses.idSegment(path, 0));
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid result id");
        } catch (SQLException e) {
            throw new ServletException("result lookup failed", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            if ("/signoff-pin".equals(path)) {
                setSignoffPin(req, resp);
            } else if ("/import-report".equals(path)) {
                importExternalReport(req, resp);
            } else if (path.endsWith("/share")) {
                share(req, resp, PortalResponses.idSegment(path, 0));
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid result id");
        } catch (SQLException e) {
            throw new ServletException("result update failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException("reference lab call interrupted", e);
        }
    }

    private boolean ordered(Physician physician, LabResult result) throws SQLException {
        Optional<LabOrder> order = bean(OrderDao.class).findById(result.orderId());
        return order.isPresent() && order.get().physicianId() == physician.id();
    }

    private void showResult(HttpServletRequest req, HttpServletResponse resp, long resultId)
            throws SQLException, IOException {
        Physician physician = AuthFilter.current(req);
        Optional<LabResult> result = bean(ResultDao.class).findById(resultId);
        if (result.isEmpty() || !ordered(physician, result.get())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setHeader("Cache-Control", "no-store");
        PortalResponses.writeJson(resp, 200, PortalResponses.result(result.get()));
    }

    private void search(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        String analyte = req.getParameter("analyte");
        if (analyte == null || analyte.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "analyte is required");
            return;
        }
        Physician physician = AuthFilter.current(req);
        PortalResponses.writeJson(resp, 200,
                PortalResponses.results(bean(ResultDao.class).searchByAnalyte(physician.id(), analyte)));
    }

    private void showHistory(HttpServletRequest req, HttpServletResponse resp, long resultId)
            throws SQLException, IOException {
        Optional<LabResult> result = bean(ResultDao.class).findById(resultId);
        if (result.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        LabOrder order = bean(OrderDao.class).findById(result.get().orderId()).orElseThrow();
        List<LabResult> history = bean(ResultDao.class)
                .historyForAnalyte(order.patientMrn(), result.get().analyte());
        resp.setHeader("Cache-Control", "no-store");
        PortalResponses.writeJson(resp, 200, PortalResponses.results(history));
    }

    private void share(HttpServletRequest req, HttpServletResponse resp, long resultId)
            throws SQLException, IOException {
        Physician physician = AuthFilter.current(req);
        Optional<LabResult> result = bean(ResultDao.class).findById(resultId);
        if (result.isEmpty() || !ordered(physician, result.get())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String code = bean(TokenService.class).createShareCode(resultId);
        PortalResponses.writeJson(resp, HttpServletResponse.SC_CREATED, "{\"code\":" + PortalResponses.json(code) + "}");
    }

    private void setSignoffPin(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Physician physician = AuthFilter.current(req);
        try {
            String hashed = bean(PasswordHasher.class).hashSignoffPin(req.getParameter("pin"));
            bean(PhysicianDao.class).updateSignoffPin(physician.id(), hashed);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void importExternalReport(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, InterruptedException {
        String reportUrl = req.getParameter("reportUrl");
        if (reportUrl == null || !reportUrl.startsWith("http")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "reportUrl must be an http(s) URL");
            return;
        }
        byte[] pdf = bean(ReferenceLabClient.class).fetchExternalReport(reportUrl);
        resp.setContentType("application/pdf");
        resp.getOutputStream().write(pdf);
    }
}
