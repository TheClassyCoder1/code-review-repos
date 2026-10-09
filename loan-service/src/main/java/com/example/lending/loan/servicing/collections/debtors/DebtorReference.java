package com.example.lending.loan.servicing.collections.debtors;

import com.example.lending.loan.servicing.collections.debtors.IdScheme.IdentifiableProperty;

/** Debtor as referenced from a receivable or a placement file. */
public class DebtorReference {

    private final String id;
    private final String displayName;
    private final String accountNumber;
    private final String externalReference;

    public DebtorReference(String id, String displayName, String accountNumber, String externalReference) {
        this.id = id;
        this.displayName = displayName;
        this.accountNumber = accountNumber;
        this.externalReference = externalReference;
    }

    public static DebtorReference of(Debtor debtor, String externalReference) {
        return new DebtorReference(debtor.getIdentifier(), debtor.getDisplayName(),
                debtor.getIdentifier(), externalReference);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPropertyValue(IdScheme idScheme) {
        return switch (idScheme.getProperty()) {
            case ID -> id;
            case NAME -> displayName;
            case ACCOUNT_NUMBER -> accountNumber;
            case EXTERNAL_REFERENCE -> externalReference;
        };
    }

    public String getDisplayPropertyValue(IdScheme idScheme) {
        if (idScheme.is(IdentifiableProperty.NAME)) {
            return getDisplayName();
        } else {
            return getPropertyValue(idScheme);
        }
    }
}
