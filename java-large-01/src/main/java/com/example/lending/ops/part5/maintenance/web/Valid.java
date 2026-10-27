package com.example.lending.ops.part5.maintenance.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents that a request object enforces its own constraints. Page requests
 * clamp page number and size in their setters, so no validator is needed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Valid {
}
