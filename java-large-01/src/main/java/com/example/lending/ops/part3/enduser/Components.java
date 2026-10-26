package com.example.lending.ops.part3.enduser;

import java.io.Serializable;
import java.util.*;

/** Small server-side component model behind the borrower self-service profile pages. */
public final class Components {
    private Components() { }
    public interface IModel<T> extends Serializable { T getObject(); }
    public record Model<T extends Serializable>(T getObject) implements IModel<T> { }
    public record ResourceModel(String key, String defaultValue) implements IModel<String> {
        public String getObject() { return "derived.emptyvalue.message".equals(key) ? "not calculated yet" : defaultValue; }
    }
    public record MarkupStream(String markup) { }
    public static final class ComponentTag extends HashMap<String, String> { }
    public static final class StringUtils { public static final String EMPTY = ""; }
    public static class Panel implements Serializable {
        protected final java.util.List<Object> children = new ArrayList<>();
        public Panel(String id) { }
        public void add(Object child) { children.add(child); }
    }
    public record ListItem<T>(T getModelObject, java.util.List<Object> children) {
        public void add(Object child) { children.add(child); }
    }
    public abstract static class ListView<T> extends Panel {
        protected ListView(String id, IModel<? extends java.util.List<T>> model) {
            super(id);
            for (T element : model.getObject()) {
                ListItem<T> item = new ListItem<>(element, new ArrayList<>());
                populateItem(item);
                add(item);
            }
        }
        public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) { }
        protected abstract void populateItem(ListItem<T> item);
    }
    public static final class AjaxTextFieldPanel extends Panel {
        private final String label;
        private final IModel<String> model;
        private boolean enabled = true, required, outputMarkupId;
        public AjaxTextFieldPanel(String id, String label, IModel<String> model, boolean enableOnChange) { super(id); this.label = label; this.model = model; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setRequired(boolean required) { this.required = required; }
        public void setOutputMarkupId(boolean outputMarkupId) { this.outputMarkupId = outputMarkupId; }
        public String render() { return label + ": " + model.getObject() + (enabled ? "" : " (read-only)"); }
    }
    public static final class SyncopeEnduserSession {
        private static final ThreadLocal<Locale> LOCALE = ThreadLocal.withInitial(() -> Locale.ENGLISH);
        public static SyncopeEnduserSession get() { return new SyncopeEnduserSession(); }
        public Locale getLocale() { return LOCALE.get(); }
    }
}
