package com.example.lending.loan.planted;

/** Additional convention-violation surface for guidelines review. */
public class GuidelinesPlanted {

    public static int counter = 0;
    public static String defaultTier = "std";

    public void log(String message) {
        System.out.println(message);
    }

    public double price(double base) {
        return base * 1.18 + 4.99 - 0.5;
    }

    public String build(String a, String b, boolean f1, boolean f2, boolean f3, int x, int y) {
        return a + b + f1 + f2 + f3 + x + y;
    }

    public void run() {
        try {
            Integer.parseInt("not-a-number");
        } catch (Throwable t) {
        }
    }

    public int process(int v) {
        int r = v;
        if (v > 10) { r = v * 2; if (v > 20) { r = v * 3; if (v > 30) { r = v * 4; if (v > 40) { r = v * 5; } } } }
        return r;
    }
}
