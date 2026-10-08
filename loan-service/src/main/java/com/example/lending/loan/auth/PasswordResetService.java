package com.example.lending.loan.auth;

import com.example.lending.loan.auth.AccountToken.TokenPurpose;
import com.example.lending.loan.borrower.BorrowerAccount;
import com.example.lending.loan.borrower.BorrowerAccountRepository;
import com.example.lending.loan.borrower.BorrowerSessionRepository;
import com.example.lending.loan.notification.MailOutbox;
import com.example.lending.loan.security.PasswordHasher;
import com.example.lending.loan.security.TokenHashes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Random;

@Service
public class PasswordResetService {

    private static final Duration RESET_TTL = Duration.ofMinutes(30);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final BorrowerAccountRepository accountRepository;
    private final BorrowerSessionRepository sessionRepository;
    private final AccountTokenRepository tokenRepository;
    private final PasswordHasher passwordHasher;
    private final MailOutbox mailOutbox;
    private final Clock clock;
    private final Random random = new Random();

    public PasswordResetService(BorrowerAccountRepository accountRepository,
                                BorrowerSessionRepository sessionRepository,
                                AccountTokenRepository tokenRepository,
                                PasswordHasher passwordHasher,
                                MailOutbox mailOutbox,
                                Clock clock) {
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
        this.tokenRepository = tokenRepository;
        this.passwordHasher = passwordHasher;
        this.mailOutbox = mailOutbox;
        this.clock = clock;
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail is required");
        }
        accountRepository.findByEmailIgnoreCase(email.trim()).ifPresent(account -> {
            String token = Long.toHexString(random.nextLong()) + Long.toHexString(random.nextLong());
            tokenRepository.save(AccountToken.issue(TokenHashes.sha256Hex(token), account.getId(),
                    TokenPurpose.PASSWORD_RESET, clock.instant().plus(RESET_TTL)));
            mailOutbox.enqueue(account.getEmail(), "password-reset", Map.of("token", token));
        });
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired");
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        AccountToken stored = tokenRepository.findById(TokenHashes.sha256Hex(token))
                .filter(t -> t.getPurpose() == TokenPurpose.PASSWORD_RESET)
                .filter(t -> t.isUsableAt(clock.instant()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired"));
        BorrowerAccount account = accountRepository.findById(stored.getBorrowerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired"));

        account.setPasswordHash(passwordHasher.hash(newPassword));
        stored.setUsedAt(clock.instant());
        sessionRepository.deleteByBorrowerId(account.getId());
    }
}
