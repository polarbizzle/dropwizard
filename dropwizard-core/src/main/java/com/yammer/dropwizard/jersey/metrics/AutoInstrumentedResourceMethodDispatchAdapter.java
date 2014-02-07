package com.yammer.dropwizard.jersey.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.sun.jersey.spi.container.ResourceMethodDispatchAdapter;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;

import javax.ws.rs.ext.Provider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A provider that wraps a {@link com.sun.jersey.spi.container.ResourceMethodDispatchProvider} in an
 * {@link com.yammer.dropwizard.jersey.metrics.AutoInstrumentedResourceMethodDispatchProvider}
 *
 * Adapted from: {@link com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter}
 */
@Provider
public class AutoInstrumentedResourceMethodDispatchAdapter implements ResourceMethodDispatchAdapter {
    private final MetricRegistry registry;

    /**
     * Construct a resource method dispatch adapter using the given metrics registry name.
     *
     * @param registryName the name of a shared metric registry
     */
    public AutoInstrumentedResourceMethodDispatchAdapter(String registryName) {
        this(SharedMetricRegistries.getOrCreate(checkNotNull(registryName)));
    }

    /**
     * Construct a resource method dispatch adapter using the given metrics registry.
     * <p/>
     * When using this constructor, the {@link AutoInstrumentedResourceMethodDispatchAdapter}
     * should be added to a Jersey {@code ResourceConfig} as a singleton.
     *
     * @param registry a {@link MetricRegistry}
     */
    public AutoInstrumentedResourceMethodDispatchAdapter(MetricRegistry registry) {
        this.registry = checkNotNull(registry);
    }


    @Override
    public ResourceMethodDispatchProvider adapt(ResourceMethodDispatchProvider provider) {
        return new AutoInstrumentedResourceMethodDispatchProvider(provider, registry);
    }
}