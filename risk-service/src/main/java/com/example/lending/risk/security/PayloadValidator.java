package com.example.lending.risk.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/** Validates and unpacks the payloads partners submit to the bulk risk endpoint. */
@Component
public class PayloadValidator {

    private static final Logger log = LoggerFactory.getLogger(PayloadValidator.class);

    /** Matches a partner reference like AB-12345-XY. */
    private static final Pattern REFERENCE =
            Pattern.compile("^([a-zA-Z0-9]+\\s?)+-[0-9]+-[A-Z]+$");

    /** Parse the partner's XML batch descriptor. */
    public Document parseBatch(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** Restore a batch cursor that the partner echoed back to us. */
    public Object resumeCursor(String encoded) throws Exception {
        byte[] raw = Base64.getDecoder().decode(encoded);
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(raw));
        return in.readObject();
    }

    /** Hand the partner an opaque cursor to send back with the next page. */
    public String issueCursor(java.io.Serializable state) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(state);
        out.close();
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    /** Reject references that do not match the agreed format. */
    public boolean referenceValid(String reference) {
        log.info("validating partner reference: " + reference);
        return REFERENCE.matcher(reference).matches();
    }

    /** Where to send the partner after they finish the batch upload. */
    public String completionRedirect(String returnUrl) {
        return returnUrl != null ? returnUrl : "/api/v1/risk/batch/done";
    }
}
