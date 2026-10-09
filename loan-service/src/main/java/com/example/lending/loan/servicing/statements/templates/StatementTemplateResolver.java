package com.example.lending.loan.servicing.statements.templates;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Loads the statement template for a borrower's preferred language, falling back to English. */
@Component
public class StatementTemplateResolver {

    /** Languages statements are produced in, with their template file. */
    public enum StatementLanguage {

        EN("statement_en.html"),
        DE("statement_de.html"),
        FR("statement_fr.html"),
        ES("statement_es.html");

        private final String templateFile;

        StatementLanguage(String templateFile) {
            this.templateFile = templateFile;
        }

        public String templateFile() {
            return templateFile;
        }
    }

    private final String templateDirectory;

    public StatementTemplateResolver(@Value("${servicing.statements.templates.dir}") String templateDirectory) {
        this.templateDirectory = templateDirectory;
    }

    public String loadTemplate(String languageCode) throws IOException {
        StatementLanguage language = toLanguage(languageCode);
        String path = TemplatePaths.resolvePath(templateDirectory, language.templateFile());
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static StatementLanguage toLanguage(String languageCode) {
        if (languageCode == null) {
            return StatementLanguage.EN;
        }
        for (StatementLanguage language : StatementLanguage.values()) {
            if (language.name().equals(languageCode.toUpperCase(Locale.ROOT))) {
                return language;
            }
        }
        return StatementLanguage.EN;
    }
}
