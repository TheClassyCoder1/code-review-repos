package com.example.lending.loan.document.search;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/search")
public class SearchMigrationController {

    private final SearchIndexMigrationRunner runner;

    public SearchMigrationController(SearchIndexMigrationRunner runner) {
        this.runner = runner;
    }

    @PostMapping("/migrate")
    public ResponseEntity<Void> migrate() throws Exception {
        runner.migrate();
        return ResponseEntity.noContent().build();
    }
}
