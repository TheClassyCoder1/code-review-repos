package com.example.lending.ops.part3.accesskey;

import com.example.lending.ops.part3.accesskey.AuditService.Audit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues partner API keys; each partner app may hold a bounded number of keys to allow rotation. */
@Service
@RequiredArgsConstructor
public class AccessKeyService {
  private static final int ACCESSKEY_COUNT_LIMIT = 5;
  private final AccessKeyRepository accessKeyRepository;
  private final AuditService auditService;
  @Transactional
  public AccessKey create(String appId, AccessKey entity) {
    long count = accessKeyRepository.countByAppId(appId);
    if (count >= ACCESSKEY_COUNT_LIMIT) {
      throw new BadRequestException("AccessKeys count limit exceeded");
    }

    entity.setId(0L);
    entity.setAppId(appId);
    entity.setDataChangeLastModifiedBy(entity.getDataChangeCreatedBy());
    AccessKey accessKey = accessKeyRepository.save(entity);

    auditService.audit(AccessKey.class.getSimpleName(), accessKey.getId(), Audit.OP.INSERT,
        accessKey.getDataChangeCreatedBy());

    return accessKey;
  }
  public static class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
  }
}
