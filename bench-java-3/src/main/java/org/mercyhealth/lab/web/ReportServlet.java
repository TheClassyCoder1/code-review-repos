package org.mercyhealth.lab.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.mercyhealth.lab.LabBootstrap;
import org.mercyhealth.lab.dao.OrderDao;
import org.mercyhealth.lab.dao.ResultDao;
import org.mercyhealth.lab.model.Department;
import org.mercyhealth.lab.model.LabOrder;
import org.mercyhealth.lab.model.LabResult;
import org.mercyhealth.lab.model.Physician;
import org.mercyhealth.lab.model.ResultStatus;
import org.mercyhealth.lab.service.AnalyzerBridge;

@WebServlet("/portal/reports/*")
public class ReportServlet extends HttpServlet {

    private static final int DEPARTMENT_LIMIT = 100;

    private <T> T bean(Class<T> type) {
        return LabBootstrap.lookup(getServletContext(), type);
    }

    static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length());
        for (char c : raw.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            if ("/department".equals(path)) {
                departmentFeed(req, resp);
            } else if (path.startsWith("/order/")) {
                orderReport(req, resp, PortalResponses.idSegment(path, 1));
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid order id");
        } catch (SQLException e) {
            throw new ServletException("report failed", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        if (!path.startsWith("/order/") || !path.endsWith("/pdf")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            long orderId = PortalResponses.idSegment(path, 1);
            if (ownOrder(req, orderId).isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String title = Optional.ofNullable(req.getParameter("title")).orElse("Cumulative report");
            Path pdf = bean(AnalyzerBridge.class).exportCumulativeReport(orderId, title);
            resp.setContentType("application/pdf");
            Files.copy(pdf, resp.getOutputStream());
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid order id");
        } catch (SQLException e) {
            throw new ServletException("report export failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException("report export interrupted", e);
        }
    }

    private Optional<LabOrder> ownOrder(HttpServletRequest req, long orderId) throws SQLException {
        Physician physician = AuthFilter.current(req);
        return bean(OrderDao.class).findById(orderId).filter(o -> o.physicianId() == physician.id());
    }

    private void departmentFeed(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Department department;
        try {
            department = Department.valueOf(String.valueOf(req.getParameter("dept")).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "unknown department");
            return;
        }
        PortalResponses.writeJson(resp, 200, PortalResponses.results(
                bean(ResultDao.class).recentForDepartment(department, DEPARTMENT_LIMIT)));
    }

    static String statusBadge(ResultStatus status) {
        if (status == null) {
            return "";
        }
        return "<span class=\"badge " + status.cssClass() + "\">" + status.label() + "</span>";
    }

    private void orderReport(HttpServletRequest req, HttpServletResponse resp, long orderId)
            throws SQLException, IOException {
        Optional<LabOrder> order = ownOrder(req, orderId);
        if (order.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<LabResult> results = bean(ResultDao.class).findByOrder(orderId);
        Physician physician = AuthFilter.current(req);
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>Lab report</title></head><body>");
        out.println("<h1>Order " + orderId + " &middot; MRN " + escape(order.get().patientMrn()) + "</h1>");
        if (results.isEmpty()) {
            out.println(emptyState());
        }
        out.println("<table class=\"results\">");
        for (LabResult result : results) {
            out.println(renderResultRow(result));
        }
        out.println("</table>");
        for (LabResult result : results) {
            if (result.comment() != null && !result.comment().isBlank()) {
                out.println(renderComment(result, physician.fullName()));
            }
        }
        out.println(reportFooter(physician));
        out.println("</body></html>");
    }

    static String renderResultRow(LabResult result) {
        StringBuilder row = new StringBuilder("<tr>");
        row.append("<td>").append(escape(result.analyte())).append("</td>");
        row.append("<td class=\"value\">").append(escape(result.value())).append("</td>");
        row.append("<td>").append(escape(result.unit())).append("</td>");
        row.append("<td>").append(statusBadge(result.status())).append("</td>");
        row.append("<td>").append(escape(result.resultedAt().toString())).append("</td>");
        return row.append("</tr>").toString();
    }

    static String reportFooter(Physician physician) {
        return "<footer>Printed for " + escape(physician.fullName()) + "</footer>";
    }

    static String emptyState() {
        return "<p class=\"empty\">No results have been reported for this order yet.</p>";
    }

    static String renderComment(LabResult result, String authorName) {
        StringBuilder html = new StringBuilder("<div class=\"comment\">");
        html.append("<strong>").append(escape(authorName)).append("</strong>");
        html.append("<span class=\"analyte\">").append(escape(result.analyte())).append("</span>");
        html.append("<p>").append(result.comment()).append("</p>");
        return html.append("</div>").toString();
    }
}
