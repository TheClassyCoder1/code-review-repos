package org.mercyhealth.lab.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Set;

public class AllowListObjectInputStream extends ObjectInputStream {

    private static final Set<String> ALLOWED = Set.of(
            "org.mercyhealth.lab.service.SampleImport$SampleRecord",
            "org.mercyhealth.lab.service.SampleImport$SampleSnapshot",
            "java.util.ArrayList",
            "java.util.ImmutableCollections$ListN",
            "java.util.CollSer",
            "java.lang.Long",
            "java.lang.Number",
            "java.lang.String",
            "java.time.Ser");

    public AllowListObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        if (!ALLOWED.contains(desc.getName())) {
            throw new InvalidClassException(desc.getName(), "class not allowed in sample snapshot");
        }
        return super.resolveClass(desc);
    }
}
