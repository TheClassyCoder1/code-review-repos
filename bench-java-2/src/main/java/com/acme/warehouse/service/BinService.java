package com.acme.warehouse.service;

import com.acme.warehouse.model.Bin;
import com.acme.warehouse.model.NotFoundException;
import com.acme.warehouse.repository.BinRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BinService {

    private final BinRepository bins;

    public BinService(BinRepository bins) {
        this.bins = bins;
    }

    public boolean register(String code, String zone, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        return bins.createIfAbsent(code.trim().toUpperCase(Locale.ROOT), zone.trim().toUpperCase(Locale.ROOT), capacity);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public int recordPutaway(String code, int units) {
        Bin bin = bins.findByCode(code).orElseThrow(() -> new NotFoundException("bin " + code));
        if (bin.usedUnits() + units > bin.capacity()) {
            throw new IllegalStateException("bin " + code + " would exceed capacity");
        }
        return bins.addUsedUnits(code, units);
    }

    @Transactional
    public void consolidate(String fromCode, String toCode) {
        if (fromCode.equals(toCode)) {
            throw new IllegalArgumentException("source and target bin are the same");
        }
        Bin target = bins.findByCode(toCode).orElseThrow(() -> new NotFoundException("bin " + toCode));
        Bin source = bins.findByCode(fromCode).orElseThrow(() -> new NotFoundException("bin " + fromCode));
        if (target.usedUnits() + source.usedUnits() > target.capacity()) {
            throw new IllegalStateException("bin " + toCode + " cannot hold the contents of " + fromCode);
        }
        bins.moveContents(fromCode, toCode);
    }
}
