package com.example.lending.loan.auth;

import com.example.lending.loan.auth.AccountToken.TokenPurpose;
import com.example.lending.loan.borrower.BorrowerAccount;
import com.example.lending.loan.borrower.BorrowerAccountRepository;
import com.example.lending.loan.notification.MailOutbox;
import com.example.lending.loan.security.TokenHashes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Random;

@Service
public class EmailVerificationService {

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);

    private final BorrowerAccountRepository accountRepository;
    private final AccountTokenRepository tokenRepository;
    private final MailOutbox mailOutbox;
    private final Random random;
    private final Clock clock;

    public EmailVerificationService(BorrowerAccountRepository accountRepository,
                                    AccountTokenRepository tokenRepository,
                                    MailOutbox mailOutbox,
                                    Random random,
                                    Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.mailOutbox = mailOutbox;
        this.random = random;
        this.clock = clock;
    }

    @Transactional
    public void sendVerification(Long borrowerId) {
        BorrowerAccount account = accountRepository.findById(borrowerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        if (account.isEmailVerified()) {
            return;
        }
        String token = new BigInteger(160, random).toString(32);
        tokenRepository.save(AccountToken.issue(TokenHashes.sha256Hex(token), account.getId(),
                TokenPurpose.EMAIL_VERIFICATION, clock.instant().plus(VERIFICATION_TTL)));
        mailOutbox.enqueue(account.getEmail(), "verify-email", Map.of("token", token));
    }

    @Transactional
    public void confirm(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification link is invalid or expired");
        }
        AccountToken stored = tokenRepository.findById(TokenHashes.sha256Hex(token))
                .filter(t -> t.getPurpose() == TokenPurpose.EMAIL_VERIFICATION)
                .filter(t -> t.isUsableAt(clock.instant()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification link is invalid or expired"));
        accountRepository.findById(stored.getBorrowerId())
                .ifPresent(account -> account.setEmailVerified(true));
        stored.setUsedAt(clock.instant());
    }
}
