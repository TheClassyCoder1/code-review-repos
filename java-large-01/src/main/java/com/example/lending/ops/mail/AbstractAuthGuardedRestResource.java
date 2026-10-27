package com.example.lending.ops.mail;

import com.example.lending.ops.security.LoginUserUtils.PermissionService;
import org.springframework.http.ResponseEntity;
import java.io.*;
import java.util.Map;

public abstract class AbstractAuthGuardedRestResource {

  public boolean hasWriteAuthority() {
    return new PermissionService().hasPermission("platform:write");
  }

  protected ResponseEntity<Void> ok() {
    return ResponseEntity.ok().build();
  }

  protected ResponseEntity<Map<String, String>> badRequest(Exception e) {
    StringWriter trace = new StringWriter();
    e.printStackTrace(new PrintWriter(trace));
    return ResponseEntity.badRequest().body(Map.of("description", String.valueOf(e.getMessage()), "stackTrace", trace.toString()));
  }

  protected ResponseEntity<Map<String, String>> serverError(String message) {
    return ResponseEntity.internalServerError().body(Map.of("description", message));
  }
}
