package com.example.lending.loan.planted;

import org.springframework.stereotype.Service;

/** Additional duplication surface for redundancy review. */
@Service
public class RedundancyPlanted {

    public static final double TAX_RATE = 0.18;
    public static final double GST_RATE = 0.18;

    public boolean blank(String s) {
        return s == null || s.length() == 0;
    }

    public double netA(double x) {
        return x - x * 0.18;
    }

    public double netB(double x) {
        return x - x * 0.18;
    }

    public int bigger(int a, int b) {
        if (a > b) {
            return a;
        } else {
            return b;
        }
    }

    public String trimName(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    public String trimLabel(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
