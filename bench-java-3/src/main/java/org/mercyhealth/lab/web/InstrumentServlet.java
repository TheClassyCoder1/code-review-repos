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
import java.util.List;
import java.util.regex.Pattern;
import org.mercyhealth.lab.LabBootstrap;
import org.mercyhealth.lab.service.AnalyzerBridge;
import org.mercyhealth.lab.service.SampleImport;

@WebServlet("/portal/instruments/*")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024)
public class InstrumentServlet extends HttpServlet {

    private static final Pattern INSTRUMENT_ID = Pattern.compile("[A-Z]{2}-[0-9]{3}");

    private <T> T bean(Class<T> type) {
        return LabBootstrap.lookup(getServletContext(), type);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            if ("/batch".equals(path)) {
                importBatch(req, resp);
            } else if (path.endsWith("/rerun")) {
                rerun(req, resp, path.substring(1, path.length() - "/rerun".length()));
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException("analyzer call interrupted", e);
        }
    }

    private void rerun(HttpServletRequest req, HttpServletResponse resp, String instrumentId)
            throws IOException, InterruptedException {
        if (!INSTRUMENT_ID.matcher(instrumentId).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "unknown instrument");
            return;
        }
        long sampleId;
        try {
            sampleId = Long.parseLong(req.getParameter("sampleId"));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "sampleId must be numeric");
            return;
        }
        String output = bean(AnalyzerBridge.class).rerunSample(instrumentId, sampleId);
        PortalResponses.writeJson(resp, 202, "{\"analyzer\":" + PortalResponses.json(output) + "}");
    }

    private void importBatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Part batch = req.getPart("batch");
        if (batch == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "batch part is required");
            return;
        }
        List<SampleImport.SampleRecord> samples;
        try (InputStream in = batch.getInputStream()) {
            samples = bean(SampleImport.class).readAnalyzerBatch(in);
        }
        PortalResponses.writeJson(resp, 200, "{\"imported\":" + samples.size() + "}");
    }
}
