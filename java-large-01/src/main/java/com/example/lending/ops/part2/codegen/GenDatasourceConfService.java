package com.example.lending.ops.part2.codegen;

import lombok.Data;

public interface GenDatasourceConfService {
    /** Saves an edited reporting datasource; a blank password keeps the stored one. */
    Boolean updateDsByEnc(GenDatasourceConf conf);

    /** A reporting datasource registered for the report builder; the password is stored encrypted. */
    @Data
    class GenDatasourceConf { private Long id; private String name, url, username, password; }
}
