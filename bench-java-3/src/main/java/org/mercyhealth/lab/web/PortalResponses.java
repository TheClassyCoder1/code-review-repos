package org.mercyhealth.lab.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.mercyhealth.lab.model.LabOrder;
import org.mercyhealth.lab.model.LabResult;

final class PortalResponses {

    private PortalResponses() {
    }

    static String json(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    static String order(LabOrder o) {
        return "{\"id\":" + o.id() + ",\"patient_mrn\":" + json(o.patientMrn())
                + ",\"test_code\":" + json(o.testCode()) + ",\"priority\":" + json(o.priority())
                + ",\"status\":" + json(o.status()) + ",\"ordered_at\":" + json(o.orderedAt().toString()) + "}";
    }

    static String result(LabResult r) {
        return "{\"id\":" + r.id() + ",\"order_id\":" + r.orderId() + ",\"analyte\":" + json(r.analyte())
                + ",\"value\":" + json(r.value()) + ",\"unit\":" + json(r.unit())
                + ",\"status\":" + json(r.status().name()) + ",\"resulted_at\":"
                + json(r.resultedAt().toString()) + "}";
    }

    static String orders(List<LabOrder> orders) {
        return orders.stream().map(PortalResponses::order).collect(Collectors.joining(",", "[", "]"));
    }

    static String results(List<LabResult> results) {
        return results.stream().map(PortalResponses::result).collect(Collectors.joining(",", "[", "]"));
    }

    static void writeJson(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(body);
    }

    static long idSegment(String pathInfo, int index) {
        String[] parts = pathInfo == null ? new String[0] : pathInfo.substring(1).split("/");
        if (parts.length <= index) {
            throw new NumberFormatException("missing id segment");
        }
        return Long.parseLong(parts[index]);
    }
}
