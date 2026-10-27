package com.example.lending.loan.partner.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

@Service
public class PartnerCredentialStore {

    private static final Logger LOG = LoggerFactory.getLogger(PartnerCredentialStore.class);

    private final PartnerUserRepository partnerUserRepository;

    public PartnerCredentialStore(PartnerUserRepository partnerUserRepository) {
        this.partnerUserRepository = partnerUserRepository;
    }

    @Transactional
    public void setPassword(Long userId, String password) {
        PartnerUser user = partnerUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown partner user " + userId));
        user.setPasswordHash(getHash(password));
    }

    protected String getHash( final String text ) {
        try {
            return CryptoUtil.getPbkdf2SaltedPassword( text.getBytes( StandardCharsets.UTF_8 ) );
        } catch( final NoSuchAlgorithmException e ) {
            LOG.error( "Error creating salted password hash: {}", e.getMessage() );
            return text;
        }
    }
}
