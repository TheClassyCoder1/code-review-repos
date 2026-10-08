package com.example.lending.loan.document.render;

import org.springframework.web.util.HtmlUtils;

import java.util.Map;

/** Server-side definition of a statement form field. Values taken from the render context are HTML-escaped. */
public class FormField {

    private final String name;
    private final String title;
    private final String event;
    private final String action;
    private final String tooltip;

    public FormField(String name, String title, String event, String action, String tooltip) {
        this.name = name;
        this.title = title;
        this.event = event;
        this.action = action;
        this.tooltip = tooltip;
    }

    public String getEntry(Map<String, Object> context, String defaultValue) {
        Object raw = context.get(name);
        String value = raw == null ? defaultValue : raw.toString();
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }

    public String getTitle(Map<String, Object> context) {
        return title;
    }

    public String getEvent() {
        return event;
    }

    public String getAction(Map<String, Object> context) {
        return action;
    }

    public String getTooltip(Map<String, Object> context) {
        return tooltip;
    }
}
