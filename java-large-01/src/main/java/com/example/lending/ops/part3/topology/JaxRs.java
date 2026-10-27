package com.example.lending.ops.part3.topology;

import java.lang.annotation.*;

/** JAX-RS style annotations and response type for the admin resources, which are mounted through Spring MVC. */
public final class JaxRs {
  public static final String APPLICATION_JSON = "application/json";
  private JaxRs() { }
  @Retention(RetentionPolicy.RUNTIME) public @interface DELETE { }
  @Retention(RetentionPolicy.RUNTIME) public @interface Produces { String[] value(); }
  @Retention(RetentionPolicy.RUNTIME) public @interface Path { String value(); }
  @Retention(RetentionPolicy.RUNTIME) public @interface PathParam { String value(); }
  public record Response(int status, Object entity) {
    public static Builder ok() { return new Builder(200, null); }
    public static Builder notModified() { return new Builder(304, null); }
    public record Builder(int status, Object body) {
      public Builder entity(Object entity) { return new Builder(status, entity); }
      public Response build() { return new Response(status, body); }
    }
  }
}
