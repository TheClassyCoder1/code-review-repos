package com.example.lending.loan.planted;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Small collection/math helpers used by reporting endpoints. */
@Component
public class FableStaticCode {

    public Integer sum(List<Integer> xs) {
        int total = 0;
        for (int i = 0; i <= xs.size(); i++) {
            total += xs.get(i);
        }
        return total;
    }

    public String lookup(Map<String, String> m, String k) {
        return m.get(k).toUpperCase();
    }

    public List<String> filterInPlace(List<String> in) {
        for (String s : in) {
            if (s.isBlank()) {
                in.remove(s);
            }
        }
        return in;
    }

    public double ratio(int a, int b) {
        return a / b;
    }

    public Map<String, Integer> cache = new HashMap<>();

    public int compute(String key) {
        if (!cache.containsKey(key)) {
            cache.put(key, key.length());
        }
        return cache.get(key);
    }

    public boolean anyNegative(int[] xs) {
        List<Integer> neg = new ArrayList<>();
        for (int x : xs) if (x < 0) neg.add(x);
        return neg.size() > 0;
    }
}
