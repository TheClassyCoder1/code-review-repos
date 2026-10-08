package com.example.lending.loan.document.render;

import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/** Renders statement form fields to HTML for the borrower statement pages. */
public class StatementFormRenderer {

    private final String contentPrefix;

    public StatementFormRenderer(String contentPrefix) {
        this.contentPrefix = contentPrefix;
    }

    public void renderImageField(Appendable writer, Map<String, Object> context, ImageField imageField) throws IOException {
        FormField modelFormField = imageField.getModelFormField();
        String value = modelFormField.getEntry(context, imageField.getValue(context));
        String description = imageField.getDescription(context);
        String alternate = imageField.getAlternate(context);
        String style = imageField.getStyle(context);
        if (description == null || description.isEmpty()) {
            description = imageField.getModelFormField().getTitle(context);
        }
        if (alternate == null || alternate.isEmpty()) {
            alternate = description;
        }
        if (value != null && !value.isEmpty()) {
            if (!value.startsWith("http")) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(contentPrefix);
                buffer.append(value);
                value = buffer.toString();
            }
        } else if (value == null) {
            value = "";
        }
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        StringWriter sr = new StringWriter();
        sr.append("<img ");
        sr.append(" src=\"");
        sr.append(value);
        sr.append("\" title=\"");
        sr.append(encode(description));
        sr.append("\" alt=\"");
        sr.append(encode(alternate));
        sr.append("\" class=\"");
        sr.append(style);
        sr.append("\" data-event=\"");
        sr.append(event == null ? "" : event);
        sr.append("\" data-action=\"");
        sr.append(action == null ? "" : action);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.appendTooltip(writer, context, modelFormField);
    }

    private static String encode(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }

    private static void executeMacro(Appendable writer, String markup) throws IOException {
        writer.append(markup);
    }

    private void appendTooltip(Appendable writer, Map<String, Object> context, FormField modelFormField) throws IOException {
        String tooltip = modelFormField.getTooltip(context);
        if (tooltip != null && !tooltip.isEmpty()) {
            writer.append("<span class=\"tooltip\">").append(encode(tooltip)).append("</span>");
        }
    }
}
