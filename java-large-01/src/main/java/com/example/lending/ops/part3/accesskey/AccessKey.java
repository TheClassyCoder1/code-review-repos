package com.example.lending.ops.part3.accesskey;

import lombok.Data;

/** API key a partner app (broker, collections agency) uses to call the partner gateway. */
@Data
public class AccessKey {
  private Long id;
  private String appId, secret, dataChangeCreatedBy, dataChangeLastModifiedBy;
  private boolean enabled;
}
