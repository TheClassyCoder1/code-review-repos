package com.example.lending.risk.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Applicant lookup for risk analysts. */
@RestController
@RequestMapping("/api/v1/risk/analyst")
public class AnalystController {

    private static final Logger log = LoggerFactory.getLogger(AnalystController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/applicants")
    public List<Map<String, Object>> applicants(@RequestParam String name) {
        log.info("Analyst lookup for applicant name={}", name);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, full_name, email, national_id, monthly_income FROM applicants WHERE full_name ILIKE ?",
                "%" + name + "%");
        for (Map<String, Object> row : rows) {
            log.info("Matched applicant {} <{}> national_id={}", row.get("full_name"), row.get("email"), row.get("national_id"));
        }
        return rows;
    }
}
