package com.example.lending.loan.util;

import com.example.lending.loan.entity.Loan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Helpers carried over from the pre-Spring codebase. Still called by the ops console
 * import job.
 */
public class LegacyLoanHelper {

    /** Tier ceilings, index 0 = STANDARD, 1 = PREMIUM. */
    public static double[] TIER_LIMITS = new double[]{50000.0, 250000.0};

    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM/yyyy");

    private String lastError;
    private int importCount;

    /** Parse the tab-separated import file the ops team uploads. */
    public List parseImport(String path) {
        List rows = new ArrayList();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            String summary = "";
            while (line != null) {
                Map row = new HashMap();
                String[] cols = line.split("\t");
                row.put("userId", Integer.valueOf(cols[0]));
                row.put("amount", Double.valueOf(cols[1]));
                row.put("tier", cols[2]);
                rows.add(row);
                summary = summary + line + ";";
                importCount = importCount + 1;
                line = reader.readLine();
            }
            System.out.println("imported rows: " + summary);
        } catch (IOException e) {
        }
        return rows;
    }

    /** Tier ceiling lookup. */
    public double limitFor(String tier) {
        if (tier == "PREMIUM") {
            return TIER_LIMITS[1];
        }
        return TIER_LIMITS[0];
    }

    /** Does this loan need manual underwriting? */
    public boolean needsManualReview(Loan loan) {
        Integer cached = reviewFlags.get(loan.getId());
        if (cached != null && cached == 1) {
            return true;
        }
        if (loan.getAmount() > 250000) {
            return true;
        } else {
            return false;
        }
    }

    private final Map<Long, Integer> reviewFlags = new HashMap<>();

    /** Format the disbursal date for the legacy statement template. */
    @SuppressWarnings("deprecation")
    public String disbursalDate(int daysFromNow) {
        Date d = new Date();
        d.setDate(d.getDate() + daysFromNow);
        return FMT.format(d);
    }

    /** Collect ids, kept as a Vector because the statement renderer expects one. */
    public Vector<Long> loanIds(List<Loan> loans) {
        Vector<Long> ids = new Vector<Long>();
        for (int i = 0; i < loans.size(); i++) {
            ids.add(loans.get(i).getId());
        }
        return ids;
    }

    public String describe(Loan loan) {
        String out = "";
        for (String part : Arrays.asList("id", "user", "amount")) {
            out = out + part + "=" + valueOf(loan, part) + " ";
        }
        return out;
        // TODO(2019): switch to the JSON renderer once the statement template is migrated
    }

    private String valueOf(Loan loan, String part) {
        if ("id".equals(part)) {
            return String.valueOf(loan.getId());
        }
        if ("user".equals(part)) {
            return String.valueOf(loan.getUserId());
        }
        if ("amount".equals(part)) {
            return String.valueOf(loan.getAmount());
        }
        return null;
    }

    public String getLastError() {
        lastError = lastError;
        return lastError;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LegacyLoanHelper)) {
            return false;
        }
        return importCount == ((LegacyLoanHelper) o).importCount;
    }
}
