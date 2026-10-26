package com.example.lending.ops.part3.enduser;

import com.example.lending.ops.part3.enduser.Components.IModel;
import com.example.lending.ops.part3.enduser.DerAttrs.*;
import java.util.Map;

/** Only way to build the derived-attributes panel. Attributes whose schema is not among the schemas visible to the borrower are dropped here, so every attribute the panel renders has a schema entry. */
public final class DerAttrsModelFactory {
    private DerAttrsModelFactory() { }
    public static DerAttrs build(String id, Map<String, DerSchemaTO> schemas, java.util.List<Attr> attrs) {
        List<Attr> visible = List.copyOf(attrs.stream().filter(a -> schemas.containsKey(a.getSchema())).toList());
        return new DerAttrs(id, Map.copyOf(schemas), (IModel<List<Attr>>) () -> visible);
    }
}
