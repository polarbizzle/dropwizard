package com.yammer.dropwizard.jersey.metrics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A provider that wraps Resource methods with Timing and Exception meters unless excluded with the @NonInstrumented annotation.
 *
 * Adapted from: {@link com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchProvider}
 */
public class AutoInstrumentedResourceMethodDispatchProvider implements ResourceMethodDispatchProvider {

    @VisibleForTesting
    public static class TimedRequestDispatcher implements RequestDispatcher {
        private final RequestDispatcher underlying;
        private final Timer timer;
        private final Meter exceptionMeter;

        public TimedRequestDispatcher(RequestDispatcher underlying, Timer timer, Meter exceptionMeter) {
            this.underlying = checkNotNull(underlying);
            this.timer = checkNotNull(timer);
            this.exceptionMeter = checkNotNull(exceptionMeter);
        }

        @Override
        public void dispatch(Object resource, HttpContext httpContext) {
            final Timer.Context context = timer.time();
            try {
                underlying.dispatch(resource, httpContext);
            } catch(Exception ex) {
                exceptionMeter.mark();
                Throwables.propagate(ex);
            } finally {
                context.stop();
            }
        }
    }

    /*
     * A dirty hack to allow us to throw exceptions of any type without bringing down the unsafe
     * thunder.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Exception> void throwUnchecked(Throwable e) throws T {
        throw (T) e;
    }

    private final ResourceMethodDispatchProvider provider;
    private final MetricRegistry registry;

    public AutoInstrumentedResourceMethodDispatchProvider(ResourceMethodDispatchProvider provider,
                                                          MetricRegistry registry) {
        this.provider = checkNotNull(provider);
        this.registry = checkNotNull(registry);
    }

    @Override
    public RequestDispatcher create(AbstractResourceMethod method) {
        RequestDispatcher dispatcher = provider.create(method);
        if (dispatcher == null) {
            return null;
        }

        if (!method.getMethod().isAnnotationPresent(NonInstrumented.class)) {
            final Timer timer = registry.timer(chooseName(method));
            final Meter exceptionMeter = registry.meter(chooseName(method, "exceptions"));
            dispatcher = new TimedRequestDispatcher(dispatcher, timer, exceptionMeter);
        }

        return dispatcher;
    }

    private String chooseName(AbstractResourceMethod method, String... suffixes) {

        return name(name(method.getDeclaringResource().getResourceClass(),
                method.getMethod().getName()),
                suffixes);
    }
}
