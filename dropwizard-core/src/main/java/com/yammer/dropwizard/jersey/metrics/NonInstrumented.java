package com.yammer.dropwizard.jersey.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes a Resource method from instrumentation. There should be a good reason for exclusion which
 * should be included as the value of the annotation for documentation purposes.
 * @author chris.phillips
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NonInstrumented {
    String value();
}
