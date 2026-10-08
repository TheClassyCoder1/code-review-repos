package com.example.lending.loan.notification;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/** Renders notice templates. Ops may store an override for any built-in template key. */
@Service
public class NoticeTemplateService {

    private static final Pattern TEMPLATE_KEY = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");

    private final SettingStore settingStore;
    private final NoticeTemplateTuner tuner;

    public NoticeTemplateService(SettingStore settingStore, NoticeTemplateTuner tuner) {
        this.settingStore = settingStore;
        this.tuner = tuner;
    }

    public String render(String key, Map<String, String> variables) throws Exception {
        if (!TEMPLATE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid template key");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        String override = settingStore.get("notice.template." + key);
        Document document;
        try (InputStream in = override != null
                ? new ByteArrayInputStream(override.getBytes(StandardCharsets.UTF_8))
                : new ClassPathResource("notice-templates/" + key + ".xml").getInputStream()) {
            document = tuner.updateSenderBlock(factory.newDocumentBuilder().parse(in));
        }
        NodeList bodies = document.getElementsByTagName("body");
        String body = bodies.getLength() == 0 ? "" : bodies.item(0).getTextContent().trim();
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            body = body.replace("{{" + variable.getKey() + "}}", variable.getValue());
        }
        return body;
    }
}
