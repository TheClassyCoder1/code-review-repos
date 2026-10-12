package com.example.lending.loan.servicing.security.portal;

/** Borrower signed in to the statement portal. */
public record PortalPrincipal(Long portalUserId, Long borrowerId, String username) {

    @Override
    public String toString() {
        return "PortalPrincipal[id=" + portalUserId + "]";
    }
}
