package com.example.lending.loan.servicing.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Authenticated operator. Holds ids and authorities only. */
public class OperatorPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String tenantId;
    private final String appLoginPassword;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public OperatorPrincipal(OperatorAccount account) {
        this.id = account.getId();
        this.username = account.getUsername();
        this.password = account.getPasswordHash();
        this.tenantId = account.getTenantId();
        this.appLoginPassword = account.getAppLoginPassword();
        this.enabled = !account.isDisabled();
        this.authorities = toAuthorities(account.getRoles(), account.getPermissions());
    }

    private static List<GrantedAuthority> toAuthorities(String roles, String permissions) {
        List<GrantedAuthority> result = new ArrayList<>();
        if (roles != null) {
            for (String role : roles.split(",")) {
                if (!role.isBlank()) {
                    result.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
                }
            }
        }
        if (permissions != null) {
            for (String permission : permissions.split(",")) {
                if (!permission.isBlank()) {
                    result.add(new SimpleGrantedAuthority(permission.trim()));
                }
            }
        }
        return List.copyOf(result);
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAppLoginPassword() {
        return appLoginPassword;
    }

    public boolean hasAuthority(String authority) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "OperatorPrincipal[id=" + id + "]";
    }
}
