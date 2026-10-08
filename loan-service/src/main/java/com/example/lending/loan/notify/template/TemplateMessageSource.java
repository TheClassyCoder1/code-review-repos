package com.example.lending.loan.notify.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/** Notification message bundles with runtime overrides of individual messages. */
@Component
public class TemplateMessageSource {

    private static final Logger log = LoggerFactory.getLogger(TemplateMessageSource.class);

    public record PresentationMessage(String code, String message) {
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    private final List<Resource> bundles;
    private final Map<String, Properties> overrides = new ConcurrentHashMap<>();

    public TemplateMessageSource() throws IOException {
        this.bundles = List.of(new PathMatchingResourcePatternResolver().getResources("classpath*:notifications/messages*.properties"));
    }

    public void addPresentation(PresentationMessage message) {
        Resource propertyFile = findPropertiesFileFor(message.getCode());
        if (propertyFile != null) {
            Properties props = new Properties();
            try {
                MessageFileUtil.loadProperties(props, propertyFile.getInputStream());
                props.setProperty(message.getCode(), message.getMessage());
                overrides.put(String.valueOf(propertyFile.getFilename()), props);
            } catch (Exception e) {
                log.error("Error generated", e);
            }
        }
    }

    public String getMessage(String bundleName, String code) {
        Properties props = overrides.get(bundleName);
        return props == null ? null : props.getProperty(code);
    }

    private Resource findPropertiesFileFor(String code) {
        for (Resource bundle : bundles) {
            Properties props = new Properties();
            try {
                MessageFileUtil.loadProperties(props, bundle.getInputStream());
            } catch (IOException e) {
                log.warn("Could not read message bundle {}", bundle.getFilename(), e);
                continue;
            }
            if (props.containsKey(code)) {
                return bundle;
            }
        }
        return null;
    }
}
