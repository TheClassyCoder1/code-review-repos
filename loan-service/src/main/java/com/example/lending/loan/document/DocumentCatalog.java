package com.example.lending.loan.document;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Component
public class DocumentCatalog {

    private final DocumentStorage storage;

    public DocumentCatalog(DocumentStorage storage) {
        this.storage = storage;
    }

    public List<String> listDocuments(String loanRef) {
        Set<String> documents = new TreeSet<>();
        String path = storage.getRoot() + "/catalog";
        File[] files = new File(path).listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                String fileName = file.getName();
                String[] strings = fileName.split("\\.");
                if (strings.length >= 1 && loanRef.equals(strings[0])
                        && "json".equals(strings[strings.length - 1])) {
                    documents.add(strings[1]);
                }
            }
        }
        return new ArrayList<>(documents);
    }
}
