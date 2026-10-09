package com.example.lending.loan.servicing.admin.imports;

import com.example.lending.loan.servicing.security.OperatorAccount;
import com.example.lending.loan.servicing.security.OperatorAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.util.regex.Pattern;

/** Credential operations on one operator account. */
public class OperatorCredentialManager {

    /** Per-import state: the tenant whose password policy applies to the credential being written. */
    public static class ImportContext {

        private String tenantId;

        public ImportContext(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTenant() {
            return tenantId;
        }

        public void setTenant(String tenantId) {
            this.tenantId = tenantId;
        }
    }

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final Pattern BCRYPT = Pattern.compile("^\\{bcrypt}\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final OperatorAccount user;
    private final OperatorAccountRepository operators;
    private final OperatorCredentialRepository credentials;
    private final PasswordEncoder passwordEncoder;
    private final ImportContext context;

    OperatorCredentialManager(OperatorAccount user, OperatorAccountRepository operators,
                              OperatorCredentialRepository credentials, PasswordEncoder passwordEncoder,
                              ImportContext context) {
        this.user = user;
        this.operators = operators;
        this.credentials = credentials;
        this.passwordEncoder = passwordEncoder;
        this.context = context;
    }

    public OperatorCredential getStoredCredentialById(String id) {
        return credentials.findByIdAndOperatorId(id, user.getId()).orElse(null);
    }

    public void updateCredential(String rawPassword) {
        if (context.getTenant() == null || !context.getTenant().equals(user.getTenantId())) {
            throw new ModelException("invalidTenant");
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ModelException("invalidPasswordMinLengthMessage", MIN_PASSWORD_LENGTH);
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        operators.save(user);
    }

    public void createCredentialThroughProvider(OperatorCredential credential) {
        if (!BCRYPT.matcher(credential.getSecretData()).matches()) {
            throw new ModelException("Only bcrypt hashes can be imported");
        }
        credential.setId(credential.getId() == null ? UUID.randomUUID().toString() : credential.getId());
        credential.setOperatorId(user.getId());
        credentials.save(credential);
        user.setPasswordHash(credential.getSecretData());
        operators.save(user);
    }
}
