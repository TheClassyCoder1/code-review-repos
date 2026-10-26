package com.example.lending.ops.part4.members;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsHomeAdvertiseServiceImpl implements SmsHomeAdvertiseService {

    private final SmsHomeAdvertiseMapper advertiseMapper;

    @Override
    public int create(SmsHomeAdvertise advertise) {
        advertise.setClickCount(0);
        advertise.setOrderCount(0);
        return advertiseMapper.insert(advertise);
    }
}
