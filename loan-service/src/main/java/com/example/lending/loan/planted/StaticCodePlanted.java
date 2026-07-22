package com.example.lending.loan.planted;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Additional defect surface for static-analysis review. */
@Component
public class StaticCodePlanted {

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd");

    public String format(Date d) {
        return FMT.format(d);
    }

    public boolean sameId(Long a, Long b) {
        return a == b;
    }

    public boolean isApproved(String status) {
        return status == "APPROVED";
    }

    public long cents(int dollars) {
        return dollars * 100;
    }

    public int readConfig() throws IOException {
        FileInputStream f = new FileInputStream("/etc/app.conf");
        return f.read();
    }

    public int countdown(int n) {
        while (n != 0) {
            n -= 2;
        }
        return n;
    }

    public int classify(int x) {
        if (1 > 2) {
            return -1;
        }
        return x;
    }
}
