package com.example.lending.ops.common.apidoc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Endpoint metadata exported to the ops API catalogue. */
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiOperation {
    String value();
    String notes() default "";
    Class<?> response() default Void.class;

    @Retention(RetentionPolicy.RUNTIME)
    @interface ApiImplicitParams { ApiImplicitParam[] value(); }
    @Retention(RetentionPolicy.RUNTIME)
    @interface ApiImplicitParam { String name(); boolean required() default false; String dataType() default ""; Class<?> dataTypeClass() default Void.class; }
    @Retention(RetentionPolicy.RUNTIME)
    @interface ApiOperationSupport { String[] ignoreParameters() default {}; }
    @Retention(RetentionPolicy.RUNTIME)
    @interface Operation { String summary(); String description() default ""; }
    @Retention(RetentionPolicy.RUNTIME)
    @interface Parameter { String name(); String description() default ""; boolean required() default false; String example() default ""; }
}
