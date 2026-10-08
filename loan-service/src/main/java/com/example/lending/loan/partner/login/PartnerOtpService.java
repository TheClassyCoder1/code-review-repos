package com.example.lending.loan.partner.login;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class PartnerOtpService {

    private final OtpCodeCache memberCacheService;

    public PartnerOtpService(OtpCodeCache memberCacheService) {
        this.memberCacheService = memberCacheService;
    }

    public String generateAuthCode(String telephone) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i=0;i<6;i++){
            sb.append(random.nextInt(10));
        }
        memberCacheService.setAuthCode(telephone,sb.toString());
        return sb.toString();
    }

    public boolean verifyAuthCode(String telephone, String code) {
        return memberCacheService.consume(telephone, code);
    }
}
