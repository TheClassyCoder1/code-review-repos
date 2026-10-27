package com.example.lending.ops.part5.maintenance.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a handler whose invocations are written to the console access audit trail. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiAccessLog {

    OperateType operateType() default OperateType.OTHER;

    enum OperateType {
        GET, CREATE, UPDATE, DELETE, EXPORT, IMPORT, OTHER
    }
}
