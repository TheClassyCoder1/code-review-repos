package com.example.lending.loan.notify.template;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateMapper;

    public NotificationTemplateService(NotificationTemplateRepository templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Transactional
    public Long createTemplate(NotificationTemplateSaveRequest createReqVO) {
        validateTemplateEventTypeUnique(null, createReqVO.eventType());

        NotificationTemplate template = new NotificationTemplate();
        template.setEventType(createReqVO.eventType());
        template.setChannel(createReqVO.channel());
        template.setBody(createReqVO.body());
        templateMapper.save(template);
        return template.getId();
    }

    private void validateTemplateEventTypeUnique(Long id, String eventType) {
        templateMapper.findByEventType(eventType).ifPresent(existing -> {
            if (id == null || !Objects.equals(existing.getId(), id)) {
                throw new IllegalArgumentException("A template already exists for event type " + eventType);
            }
        });
    }
}
