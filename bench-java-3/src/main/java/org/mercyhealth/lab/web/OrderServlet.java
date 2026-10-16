package org.mercyhealth.lab.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.mercyhealth.lab.LabBootstrap;
import org.mercyhealth.lab.LabConfig;
import org.mercyhealth.lab.dao.OrderDao;
import org.mercyhealth.lab.dao.ResultDao;
import org.mercyhealth.lab.model.LabOrder;
import org.mercyhealth.lab.model.Physician;
import org.mercyhealth.lab.service.AttachmentStore;
import org.mercyhealth.lab.service.ReferenceLabClient;

@WebServlet("/portal/orders/*")
@MultipartConfig(maxFileSize = 20 * 1024 * 1024)
public class OrderServlet extends HttpServlet {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "ordered", "ordered_at",
            "priority", "priority",
            "status", "status",
            "test", "test_code");
    private static final Set<String> PRIORITIES = Set.of("ROUTINE", "URGENT", "STAT");
    private static final Pattern MRN = Pattern.compile("[0-9]{6,10}");
    private static final Pattern TEST_CODE = Pattern.compile("[A-Z0-9]{2,12}");
    private static final Pattern STORED_KEY =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|png|jpg|tiff)");

    private <T> T bean(Class<T> type) {
        return LabBootstrap.lookup(getServletContext(), type);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            if ("/".equals(path)) {
                listOrders(req, resp);
            } else if (path.endsWith("/results")) {
                showOrderResults(req, resp, PortalResponses.idSegment(path, 0));
            } else if (path.endsWith("/requisition")) {
                downloadRequisition(req, resp, PortalResponses.idSegment(path, 0));
            } else if (path.contains("/attachments/")) {
                downloadAttachment(req, resp, PortalResponses.idSegment(path, 0),
                        path.substring(path.lastIndexOf('/') + 1));
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid order id");
        } catch (SQLException e) {
            throw new ServletException("order lookup failed", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            if ("/".equals(path)) {
                createOrder(req, resp);
            } else if (path.endsWith("/forward")) {
                forwardToPartner(req, resp, PortalResponses.idSegment(path, 0));
            } else if (path.endsWith("/attachments")) {
                uploadAttachment(req, resp, PortalResponses.idSegment(path, 0));
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid order id");
        } catch (SQLException e) {
            throw new ServletException("order update failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException("partner call interrupted", e);
        }
    }

    private Optional<LabOrder> ownOrder(HttpServletRequest req, HttpServletResponse resp, long orderId)
            throws SQLException, IOException {
        Physician physician = AuthFilter.current(req);
        Optional<LabOrder> order = bean(OrderDao.class).findById(orderId);
        if (order.isEmpty() || order.get().physicianId() != physician.id()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return Optional.empty();
        }
        return order;
    }

    private void listOrders(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        String sortKey = req.getParameter("sort") == null ? "ordered" : req.getParameter("sort");
        String column = SORT_COLUMNS.get(sortKey);
        if (column == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "unknown sort key");
            return;
        }
        Physician physician = AuthFilter.current(req);
        PortalResponses.writeJson(resp, 200,
                PortalResponses.orders(bean(OrderDao.class).listForPhysician(physician.id(), column)));
    }

    private void showOrderResults(HttpServletRequest req, HttpServletResponse resp, long orderId)
            throws SQLException, IOException {
        if (ownOrder(req, resp, orderId).isEmpty()) {
            return;
        }
        PortalResponses.writeJson(resp, 200,
                PortalResponses.results(bean(ResultDao.class).findByOrder(orderId)));
    }

    private void downloadRequisition(HttpServletRequest req, HttpServletResponse resp, long orderId)
            throws SQLException, IOException {
        if (ownOrder(req, resp, orderId).isEmpty()) {
            return;
        }
        resp.setContentType("application/pdf");
        try (InputStream in = bean(AttachmentStore.class).openRequisition(orderId, req.getParameter("name"))) {
            in.transferTo(resp.getOutputStream());
        }
    }

    private void downloadAttachment(HttpServletRequest req, HttpServletResponse resp, long orderId,
                                    String storedKey) throws SQLException, IOException {
        if (!STORED_KEY.matcher(storedKey).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid attachment key");
            return;
        }
        if (ownOrder(req, resp, orderId).isEmpty()) {
            return;
        }
        resp.setContentType("application/octet-stream");
        resp.getOutputStream().write(bean(AttachmentStore.class).readStored(storedKey));
    }

    private void createOrder(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        String mrn = req.getParameter("patientMrn");
        String testCode = req.getParameter("testCode");
        String priority = req.getParameter("priority") == null ? "ROUTINE" : req.getParameter("priority");
        if (mrn == null || !MRN.matcher(mrn).matches()
                || testCode == null || !TEST_CODE.matcher(testCode).matches()
                || !PRIORITIES.contains(priority)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid lab order");
            return;
        }
        Physician physician = AuthFilter.current(req);
        long id = bean(OrderDao.class).insert(mrn, physician.id(), testCode, priority);
        resp.setHeader("Location", req.getContextPath() + "/portal/orders/" + id);
        PortalResponses.writeJson(resp, HttpServletResponse.SC_CREATED, "{\"id\":" + id + "}");
    }

    private void forwardToPartner(HttpServletRequest req, HttpServletResponse resp, long orderId)
            throws SQLException, IOException, InterruptedException {
        String partnerHost = req.getParameter("partnerHost");
        if (partnerHost == null || !bean(LabConfig.class).partnerHosts().contains(partnerHost)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "unknown partner lab");
            return;
        }
        Optional<LabOrder> order = ownOrder(req, resp, orderId);
        if (order.isEmpty()) {
            return;
        }
        int status = bean(ReferenceLabClient.class)
                .sendToPartner(partnerHost, "/api/orders", PortalResponses.order(order.get()));
        bean(OrderDao.class).updateStatus(orderId, "SENT_OUT");
        PortalResponses.writeJson(resp, 202, "{\"partner_status\":" + status + "}");
    }

    private void uploadAttachment(HttpServletRequest req, HttpServletResponse resp, long orderId)
            throws SQLException, IOException, ServletException {
        if (ownOrder(req, resp, orderId).isEmpty()) {
            return;
        }
        Part file = req.getPart("file");
        if (file == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "file part is required");
            return;
        }
        try (InputStream in = file.getInputStream()) {
            String key = bean(AttachmentStore.class).save(in, file.getSubmittedFileName());
            PortalResponses.writeJson(resp, HttpServletResponse.SC_CREATED, "{\"key\":" + PortalResponses.json(key) + "}");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
