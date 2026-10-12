package com.example.lending.loan.servicing.settlement.clients;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares the remote service a settlement client talks to. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SettlementEndpoint {

    /** Service name used for discovery. */
    String name();

    /** Fixed base URL; placeholders are resolved. Used only when discovery is disabled. */
    String url() default "";
}
