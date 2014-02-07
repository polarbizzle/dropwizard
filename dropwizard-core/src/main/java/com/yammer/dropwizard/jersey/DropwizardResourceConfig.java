package com.yammer.dropwizard.jersey;

import com.codahale.metrics.MetricRegistry;
import com.sun.jersey.api.core.ScanningResourceConfig;
import com.yammer.dropwizard.jersey.caching.CacheControlledResourceMethodDispatchAdapter;
import com.yammer.dropwizard.jersey.metrics.AutoInstrumentedResourceMethodDispatchAdapter;

public class DropwizardResourceConfig extends ScanningResourceConfig {
    public DropwizardResourceConfig(boolean testOnly, MetricRegistry metricRegistry) {
        super();
        getFeatures().put(FEATURE_DISABLE_WADL, Boolean.TRUE);
        if (!testOnly) {
            // create a subclass to pin it to Throwable
            getSingletons().add(new LoggingExceptionMapper<Throwable>() {});
            getSingletons().add(new InvalidEntityExceptionMapper());
            getSingletons().add(new JsonProcessingExceptionMapper());
        }
        getSingletons().add(new AutoInstrumentedResourceMethodDispatchAdapter(metricRegistry));
        getClasses().add(CacheControlledResourceMethodDispatchAdapter.class);
        getClasses().add(OptionalResourceMethodDispatchAdapter.class);
        getClasses().add(OptionalQueryParamInjectableProvider.class);
    }
}
