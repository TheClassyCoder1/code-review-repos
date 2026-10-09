package com.example.lending.loan.servicing.settlement.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

/** Writes {@code key=value} configuration files read by the settlement file agent. */
public final class SettlementPropertiesWriter {

    private static final Logger log = LoggerFactory.getLogger(SettlementPropertiesWriter.class);

    private SettlementPropertiesWriter() {
    }

    public static void writeProperties(String fileName, Map<String, Object> configMap) {
        try {
            OutputStream os = new FileOutputStream(fileName);
            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String s = key + "=" + value + "\n";
                os.write(s.getBytes());
            }
            os.flush();
            os.close();
        } catch (Exception e) {
            log.error("writeProperties error", e);
        }
    }
}
