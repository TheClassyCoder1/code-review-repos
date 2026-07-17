package com.example.lending.loan.planted;

import org.springframework.stereotype.Service;

/** Additional business-rule surface for use-case review. */
@Service
public class UseCasePlanted {

    public boolean isEligible(int creditScore) {
        return creditScore > 650;
    }

    public double totalInterest(double principal, double monthlyRate, int months) {
        return principal * monthlyRate * (months + 1);
    }

    public double finalPrice(double price, double discountRate) {
        double afterDiscount = price - price * discountRate;
        return afterDiscount - afterDiscount * discountRate;
    }

    public long refundCents(double amount) {
        return (long) Math.ceil(amount * 100);
    }

    public boolean canTransition(String from, String to) {
        return true;
    }

    public double disburse(double amount) {
        return amount;
    }

    public double applyLateFee(double balance, int daysLate) {
        if (daysLate > 30) {
            return balance;
        }
        return balance * 1.05;
    }
}
