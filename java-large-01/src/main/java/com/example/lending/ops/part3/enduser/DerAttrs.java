package com.example.lending.ops.part3.enduser;

import com.example.lending.ops.part3.enduser.Components.*;
import java.util.*;

/** Read-only derived attributes on the borrower profile (credit utilisation, months on book, ...). */
public class DerAttrs extends Panel {
    public record Attr(String getSchema, List<String> getValues) implements java.io.Serializable { }
    public record DerSchemaTO(String key, Map<Locale, String> labels) implements java.io.Serializable {
        public String getLabel(Locale locale) { return labels.getOrDefault(locale, key); }
    }
    DerAttrs(String id, Map<String, DerSchemaTO> schemas, IModel<List<Attr>> attrTOs) {
        super(id);
        add(new DerSchemas("derSchemas", schemas, attrTOs));
    }
    public static class DerSchemas extends Panel {
        public DerSchemas(
                final String id,
                final Map<String, DerSchemaTO> schemas,
                final IModel<List<Attr>> attrTOs) {
            super(id);

            add(new ListView<>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                    super.onComponentTagBody(markupStream, openTag);
                    openTag.put("class", "empty");
                }

                @Override
                protected void populateItem(final ListItem<Attr> item) {
                    Attr attrTO = item.getModelObject();

                    IModel<String> model;
                    List<String> values = attrTO.getValues();
                    if (values == null || values.isEmpty()) {
                        model = new ResourceModel("derived.emptyvalue.message", StringUtils.EMPTY);
                    } else {
                        model = new Model<>(values.getFirst());
                    }

                    AjaxTextFieldPanel panel = new AjaxTextFieldPanel(
                            "panel",
                            schemas.get(attrTO.getSchema()).getLabel(SyncopeEnduserSession.get().getLocale()),
                            model,
                            false);
                    panel.setEnabled(false);
                    panel.setRequired(true);
                    panel.setOutputMarkupId(true);
                    item.add(panel);
                }
            });
        }
    }
}
