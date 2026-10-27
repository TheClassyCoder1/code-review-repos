package com.example.lending.loan.partner.apps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppOperationLogService {

    private final AppOperationLogRepository repository;

    public AppOperationLogService(AppOperationLogRepository repository) {
        this.repository = repository;
    }

    public List<AppOperationLog> list(String appId, Long tenantId) {
        return repository.findByAppIdAndTenantId(appId, tenantId);
    }

    @Transactional
    public boolean removeById(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
