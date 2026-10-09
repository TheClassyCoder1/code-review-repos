package com.example.lending.loan.servicing.settlement.profiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

/** Reads YAML files into typed objects. */
public final class SettlementProfileLoader {

    private static final Logger log = LoggerFactory.getLogger(SettlementProfileLoader.class);

    private SettlementProfileLoader() {
    }

    public static <T> T readYaml(String path, Class<T> clazz) {
        FileReader reader = null;
        try {
            reader = new FileReader(path);
        } catch (FileNotFoundException e) {
            log.error(path + " File Not Found, ", e);
            return null;
        }

        BufferedReader buffer = new BufferedReader(reader);
        Yaml yaml = new Yaml(new Constructor(clazz, new LoaderOptions()));
        return yaml.loadAs(buffer, clazz);
    }
}
