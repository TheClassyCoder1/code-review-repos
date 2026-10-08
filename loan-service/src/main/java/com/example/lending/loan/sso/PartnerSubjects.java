package com.example.lending.loan.sso;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Set;

public final class PartnerSubjects {

    public record PartnerPrincipal(String getName) implements Principal {
    }

    public record ImpersonatedPrincipal(String getName) implements Principal {
    }

    private PartnerSubjects() {
    }

    public static String getImpersonatedPrincipalName(Subject subject) {
        String name = null;

        Set<ImpersonatedPrincipal> impPrincipals = subject.getPrincipals(ImpersonatedPrincipal.class);
        if (!impPrincipals.isEmpty()) {
            return ((Principal) impPrincipals.toArray()[0]).getName();
        }

        return name;
    }
}
