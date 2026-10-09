package com.example.lending.loan.servicing.admin.imports;

import com.example.lending.loan.servicing.admin.imports.ModelException.PasswordPolicyNotMetException;
import com.example.lending.loan.servicing.admin.imports.OperatorCredentialManager.ImportContext;
import com.example.lending.loan.servicing.admin.imports.OperatorRepresentation.CredentialRepresentation;
import com.example.lending.loan.servicing.security.OperatorAccount;
import com.example.lending.loan.servicing.security.OperatorAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Writes the credentials of an imported operator. */
@Component
public class OperatorCredentialImporter {

    private final OperatorAccountRepository operators;
    private final OperatorCredentialRepository credentials;
    private final PasswordEncoder passwordEncoder;

    public OperatorCredentialImporter(OperatorAccountRepository operators, OperatorCredentialRepository credentials,
                                      PasswordEncoder passwordEncoder) {
        this.operators = operators;
        this.credentials = credentials;
        this.passwordEncoder = passwordEncoder;
    }

    public void createCredentials(OperatorRepresentation userRep, ImportContext session, String tenantId,
                                  OperatorAccount user, boolean adminRequest) {
        convertDeprecatedCredentialsFormat(userRep);
        OperatorCredentialManager credentialManager =
                new OperatorCredentialManager(user, operators, credentials, passwordEncoder, session);
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                if (cred.getId() != null && credentialManager.getStoredCredentialById(cred.getId()) != null) {
                    continue;
                }
                if (cred.getValue() != null && !cred.getValue().isEmpty()) {
                    if (cred.getType() != null && !CredentialRepresentation.PASSWORD.equals(cred.getType())) {
                        throw new ModelException("Credential type must be '" + CredentialRepresentation.PASSWORD + "' when value is provided, but was '" + cred.getType() + "'");
                    }
                    String origTenant = session.getTenant();
                    try {
                        session.setTenant(tenantId);
                        credentialManager.updateCredential(cred.getValue());
                    } catch (ModelException ex) {
                        PasswordPolicyNotMetException passwordPolicyNotMetException = new PasswordPolicyNotMetException(ex.getMessage(), user.getUsername(), ex);
                        passwordPolicyNotMetException.setParameters(ex.getParameters());
                        throw passwordPolicyNotMetException;
                    } finally {
                        session.setTenant(origTenant);
                    }
                } else {
                    credentialManager.createCredentialThroughProvider(toModel(cred));
                }
            }
        }
    }

    private static void convertDeprecatedCredentialsFormat(OperatorRepresentation userRep) {
        if (userRep.getPassword() != null) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(userRep.getPassword());
            userRep.addCredential(credential);
            userRep.setPassword(null);
        }
    }

    private static OperatorCredential toModel(CredentialRepresentation cred) {
        OperatorCredential credential = new OperatorCredential();
        credential.setId(cred.getId());
        credential.setType(cred.getType() == null ? CredentialRepresentation.PASSWORD : cred.getType());
        credential.setSecretData(cred.getSecretData() == null ? "" : cred.getSecretData());
        return credential;
    }
}
