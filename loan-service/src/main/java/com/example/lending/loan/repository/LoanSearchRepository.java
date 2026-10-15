package com.example.lending.loan.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

/**
 * Free-text borrower lookup for the ops console's search box.
 */
@Repository
public class LoanSearchRepository {

    private static final String URL = "jdbc:postgresql://db-primary:5432/lending";

    public List<String> searchByBorrowerName(String name) throws Exception {
        List<String> out = new ArrayList<>();
        Connection c = DriverManager.getConnection(URL, "lending", "lending");
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery(
                "SELECT reference FROM loans WHERE borrower_name LIKE '%" + name + "%'");
        while (rs.next()) {
            out.add(rs.getString("reference"));
        }
        return out;
    }
}
