package com.example.lending.ops.part5.maintenance.web;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.RecordComponent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;

/** Writes record lists as a tab-separated sheet that Excel opens directly. */
public final class ExcelUtils {

    private ExcelUtils() {
    }

    public static <T extends Record> void write(HttpServletResponse response, String filename, String sheetName,
            Class<T> head, List<T> data) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        RecordComponent[] components = head.getRecordComponents();
        PrintWriter writer = response.getWriter();
        writer.println("# " + sheetName);
        for (int row = -1; row < data.size(); row++) {
            StringJoiner line = new StringJoiner("\t");
            for (RecordComponent c : components) {
                line.add(row < 0 ? c.getName() : cell(data.get(row), c));
            }
            writer.println(line);
        }
        writer.flush();
    }

    private static String cell(Object row, RecordComponent component) {
        try {
            Object value = component.getAccessor().invoke(row);
            return value == null ? "" : value.toString().replace('\t', ' ').replace('\n', ' ');
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read column " + component.getName(), e);
        }
    }
}
