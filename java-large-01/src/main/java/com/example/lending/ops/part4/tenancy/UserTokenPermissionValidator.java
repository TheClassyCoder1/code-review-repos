package com.example.lending.ops.part4.tenancy;

import java.util.Set;

/** API-token callers need the owner's permission AND a token scope covering both the operation and the app. */
public class UserTokenPermissionValidator implements PermissionValidator {

  public enum UserTokenOperation { NAMESPACE_CREATE, CONFIG_RELEASE }

  public record UserTokenScope(Set<UserTokenOperation> operations, Set<String> appIds) {
    boolean allowsOperation(UserTokenOperation operation) { return operations.contains(operation); }

    boolean allowsApp(String appId) { return appIds.contains(appId); }
  }

  private final PermissionValidator userPermissionValidator;
  private final UserTokenScope tokenScope;

  public UserTokenPermissionValidator(PermissionValidator userPermissionValidator, UserTokenScope tokenScope) {
    this.userPermissionValidator = userPermissionValidator;
    this.tokenScope = tokenScope;
  }

  private UserTokenScope scope() { return tokenScope; }

  @Override
  public boolean hasCreateNamespacePermission(String appId) {
    return userPermissionValidator.hasCreateNamespacePermission(appId)
        && scope().allowsOperation(UserTokenOperation.NAMESPACE_CREATE) && scope().allowsApp(appId);
  }
}
