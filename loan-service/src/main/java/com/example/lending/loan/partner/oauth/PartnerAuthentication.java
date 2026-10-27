package com.example.lending.loan.partner.oauth;

import java.io.Serializable;
import java.util.Set;

public record PartnerAuthentication(Long userId, Long tenantId, Set<String> scopes) implements Serializable {
}
