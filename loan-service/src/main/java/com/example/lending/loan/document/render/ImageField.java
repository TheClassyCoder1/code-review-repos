package com.example.lending.loan.document.render;

import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.regex.Pattern;

public class ImageField {

    private static final Pattern CSS_CLASSES = Pattern.compile("^[A-Za-z0-9 _-]*$");

    private final FormField modelFormField;
    private final String valueKey;
    private final String description;
    private final String alternate;
    private final String style;

    public ImageField(FormField modelFormField, String valueKey, String description, String alternate, String style) {
        this.modelFormField = modelFormField;
        this.valueKey = valueKey;
        this.description = description;
        this.alternate = alternate;
        this.style = style;
    }

    public FormField getModelFormField() {
        return modelFormField;
    }

    public String getValue(Map<String, Object> context) {
        Object value = context.get(valueKey);
        return value == null ? null : HtmlUtils.htmlEscape(value.toString());
    }

    public String getDescription(Map<String, Object> context) {
        return description;
    }

    public String getAlternate(Map<String, Object> context) {
        return alternate;
    }

    public String getStyle(Map<String, Object> context) {
        return style != null && CSS_CLASSES.matcher(style).matches() ? style : "";
    }
}
