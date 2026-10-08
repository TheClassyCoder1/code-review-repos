package com.example.lending.loan.borrower;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional
    public NotificationPreference update(Long borrowerId, PreferenceUpdateRequest request) {
        NotificationPreference preference = preferenceRepository.findByBorrowerId(borrowerId).orElseGet(() -> {
            NotificationPreference created = new NotificationPreference();
            created.setBorrowerId(borrowerId);
            return created;
        });
        BeanUtils.copyProperties(request, preference);
        return preferenceRepository.save(preference);
    }
}
