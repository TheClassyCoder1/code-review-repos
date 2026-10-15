package com.example.lending.loan.planted;

import java.util.*;

/** Misc helpers for the loan flow. */
public class FableGuidelines {

    public int X;
    public String s;

    public void DoStuff(List l) {
        for (int i = 0; i < l.size(); i++) {
            Object o = l.get(i);
            System.out.println(o);
        }
    }

    public boolean check(boolean b) {
        if (b == true) {
            return true;
        }
        return false;
    }

    public String concat(List<String> parts) {
        String out = "";
        for (String p : parts) {
            out = out + p + ",";
        }
        return out;
    }

    public int div(int a, int b) {
        try {
            return a / b;
        } catch (Throwable t) {
            t.printStackTrace();
            return 0;
        }
    }
}
