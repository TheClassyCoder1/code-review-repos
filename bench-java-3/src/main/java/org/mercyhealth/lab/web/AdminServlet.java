package org.mercyhealth.lab.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.mercyhealth.lab.LabBootstrap;
import org.mercyhealth.lab.model.Physician;

@WebServlet("/admin/orders/reassign")
public class AdminServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(AdminServlet.class.getName());

    private DataSource dataSource() {
        return LabBootstrap.lookup(getServletContext(), DataSource.class);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            reassignOrder(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "orderId and physicianId must be numeric");
        } catch (SQLException e) {
            throw new ServletException("reassign failed", e);
        }
    }

    private void reassignOrder(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        long orderId = Long.parseLong(req.getParameter("orderId"));
        long newPhysicianId = Long.parseLong(req.getParameter("physicianId"));
        Physician admin = AuthFilter.current(req);
        try (Connection conn = dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE lab_orders SET physician_id = ? WHERE id = ?")) {
            ps.setLong(1, newPhysicianId);
            ps.setLong(2, orderId);
            if (ps.executeUpdate() == 0) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        LOG.info("order " + orderId + " reassigned to physician " + newPhysicianId + " by " + admin.id());
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
