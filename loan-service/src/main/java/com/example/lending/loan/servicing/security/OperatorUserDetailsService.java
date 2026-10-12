package com.example.lending.loan.servicing.security;

import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Loads operators for HTTP basic sign-in and for the session manager. */
@Service
public class OperatorUserDetailsService implements UserDetailsService {

    private final OperatorAccountRepository repository;

    public OperatorUserDetailsService(OperatorAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public OperatorPrincipal loadUserByUsername(String username) {
        return repository.findByUsername(username)
                .map(OperatorPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown operator"));
    }

    public Optional<OperatorPrincipal> findById(long operatorId) {
        return repository.findById(operatorId).map(OperatorPrincipal::new);
    }

    public OperatorPrincipal loadById(long operatorId) {
        return repository.findById(operatorId)
                .map(OperatorPrincipal::new)
                .orElseThrow(() -> ServicingException.notFound("Operator " + operatorId + " not found"));
    }
}
