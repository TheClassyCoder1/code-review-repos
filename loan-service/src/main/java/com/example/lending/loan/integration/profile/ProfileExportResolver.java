package com.example.lending.loan.integration.profile;

import org.springframework.stereotype.Component;

@Component
public class ProfileExportResolver {

    private final ProfileSecretCipher secretCipher;

    public ProfileExportResolver(ProfileSecretCipher secretCipher) {
        this.secretCipher = secretCipher;
    }

    public IntegrationProfile modifyDocumentForExport(IntegrationProfile adapterDescription) {
        adapterDescription.setRev(null);
        adapterDescription.setSelectedEndpointUrl(null);
        adapterDescription.setRunning(false);
        secretCipher.decrypt(adapterDescription);

        return adapterDescription;
    }
}
