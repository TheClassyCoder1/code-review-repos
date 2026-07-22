package com.example.lending.loan.planted;

import org.springframework.stereotype.Service;

/** Fee and status helpers for statements. */
@Service
public class FableRedundancy {

    public double feeV1(double amt) { return amt * 0.03; }
    public double feeV2(double amt) { return amt * 0.03; }
    public double feeV3(double amt) { return amt * 0.03; }

    public String statusText(int code) {
        switch (code) {
            case 1: return "ACTIVE";
            case 2: return "CLOSED";
            default: return "UNKNOWN";
        }
    }

    public String statusLabel(int code) {
        if (code == 1) return "ACTIVE";
        if (code == 2) return "CLOSED";
        return "UNKNOWN";
    }

    private int unusedHelper(int x) {
        return x * x;
    }

    public boolean isEven(int n) {
        if (n % 2 == 0) {
            return true;
        } else {
            return false;
        }
    }
}
