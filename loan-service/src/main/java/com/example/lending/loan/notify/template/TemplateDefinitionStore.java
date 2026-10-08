package com.example.lending.loan.notify.template;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Notification template definitions shipped as {@code define/template-<name>.yml} files. */
@Component
public class TemplateDefinitionStore {

    private final Map<String, String> appDefines = new ConcurrentHashMap<>();

    public void register(String name, String definition) {
        appDefines.put(name.toLowerCase(), definition);
    }

    public String get(String name) {
        return appDefines.get(name.toLowerCase());
    }

    public void delete(String app) {
        var rootUrl = this.getClass().getClassLoader().getResource("");
        if (rootUrl == null) return;
        var file = new File(rootUrl.getPath() + "define" + File.separator + "template-" + app + ".yml");
        if (file.exists()) file.delete();
        appDefines.remove(app.toLowerCase());
    }
}
