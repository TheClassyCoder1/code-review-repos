package com.example.lending.loan.servicing.statements.templates;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/** Checks statement template bundles (jar files with a template manifest). */
public final class TemplateBundles {

    static final String VERSION_ATTRIBUTE = "Statement-Template-Version";

    private TemplateBundles() {
    }

    public static void requireCheckBundleFile(URL bundle) throws IOException {
        try (InputStream in = bundle.openStream(); JarInputStream jar = new JarInputStream(in)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                throw new IllegalArgumentException("Template bundle has no manifest");
            }
            String version = manifest.getMainAttributes().getValue(new Attributes.Name(VERSION_ATTRIBUTE));
            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("Template bundle has no " + VERSION_ATTRIBUTE);
            }
        }
    }
}
