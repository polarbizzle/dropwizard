package com.yammer.dropwizard.jersey.metrics.tests;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractResource;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import com.yammer.dropwizard.jersey.metrics.AutoInstrumentedResourceMethodDispatchProvider;
import com.yammer.dropwizard.jersey.metrics.AutoInstrumentedResourceMethodDispatchProvider.TimedRequestDispatcher;
import com.yammer.dropwizard.jersey.metrics.NonInstrumented;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author chris.phillips
 */
public class AutoInstrumentedMethodDispatchProviderTest {

    @Test
    public void nullResourceMethodReturnsNull() {
        ResourceMethodDispatchProvider methodDispatchProvider = mock(ResourceMethodDispatchProvider.class);

        AutoInstrumentedResourceMethodDispatchProvider provider =
                new AutoInstrumentedResourceMethodDispatchProvider(methodDispatchProvider, new MetricRegistry());

        RequestDispatcher dispatcher = provider.create(null);

        assertThat(dispatcher, nullValue(RequestDispatcher.class));
    }

    @Test
    public void notAnnotatedMethodIsInstrumented() throws Exception {
        AbstractResourceMethod resourceMethod = mock(AbstractResourceMethod.class);
        when(resourceMethod.getDeclaringResource()).thenReturn(new TestResource(String.class));
        when(resourceMethod.getMethod()).thenReturn(TestResource.class.getMethod("defaultInstrumented"));
        RequestDispatcher resourceDispatcher = mock(RequestDispatcher.class);

        ResourceMethodDispatchProvider methodDispatchProvider = mock(ResourceMethodDispatchProvider.class);
        when(methodDispatchProvider.create(any(AbstractResourceMethod.class))).thenReturn(resourceDispatcher);

        MetricRegistry metricRegistry = new MetricRegistry();
        AutoInstrumentedResourceMethodDispatchProvider provider =
                new AutoInstrumentedResourceMethodDispatchProvider(methodDispatchProvider, metricRegistry);
        RequestDispatcher resultDispatcher = provider.create(resourceMethod);

        assertThat(resultDispatcher, instanceOf(AutoInstrumentedResourceMethodDispatchProvider.TimedRequestDispatcher.class));
        assertThat(metricRegistry.getMetrics().size(), is(2));
        assertThat(metricRegistry.getTimers().size(), is(1));
        assertThat(metricRegistry.getMeters().size(), is(1));
        assertThat(metricRegistry.getMetrics().keySet(), equalTo((Set<String>) ImmutableSet
                .of("java.lang.String.defaultInstrumented", "java.lang.String.defaultInstrumented.exceptions")));

    }

    @Test
    public void annotatedMethodIsNotInstrumented() throws Exception {
        AbstractResourceMethod resourceMethod = mock(AbstractResourceMethod.class);
        when(resourceMethod.getDeclaringResource()).thenReturn(new TestResource(String.class));
        when(resourceMethod.getMethod()).thenReturn(TestResource.class.getMethod("notInstrumented"));
        RequestDispatcher resourceDispatcher = mock(RequestDispatcher.class);

        ResourceMethodDispatchProvider methodDispatchProvider = mock(ResourceMethodDispatchProvider.class);
        when(methodDispatchProvider.create(any(AbstractResourceMethod.class))).thenReturn(resourceDispatcher);

        MetricRegistry metricRegistry = new MetricRegistry();
        AutoInstrumentedResourceMethodDispatchProvider provider =
                new AutoInstrumentedResourceMethodDispatchProvider(methodDispatchProvider, metricRegistry);
        RequestDispatcher resultDispatcher = provider.create(resourceMethod);

        assertThat(resultDispatcher, sameInstance(resourceDispatcher));
        assertThat(metricRegistry.getMetrics().size(), is(0));

    }


    @Test
    public void resourceDispatchesGetTimed() {
        RequestDispatcher underlying = mock(RequestDispatcher.class);
        Timer timer = mock(Timer.class);
        Timer.Context context = mock(Timer.Context.class);
        when(timer.time()).thenReturn(context);
        Meter exceptionMeter = mock(Meter.class);
        TimedRequestDispatcher dispatcher = new TimedRequestDispatcher(underlying, timer, exceptionMeter);
        HttpContext httpContext = mock(HttpContext.class);
        dispatcher.dispatch(new Object(), httpContext);

        verify(timer).time();
        verify(context).stop();
        verifyZeroInteractions(exceptionMeter);
    }

    @Test
    public void exceptionalRequestDispatchesAreMeteredAndTimedAndPropagateExceptions() {
        RequestDispatcher underlying = mock(RequestDispatcher.class);
        doThrow(new RuntimeException()).when(underlying).dispatch(anyObject(), any(HttpContext.class));
        Timer timer = mock(Timer.class);
        Timer.Context context = mock(Timer.Context.class);
        when(timer.time()).thenReturn(context);
        Meter exceptionMeter = mock(Meter.class);
        TimedRequestDispatcher dispatcher = new TimedRequestDispatcher(underlying, timer, exceptionMeter);
        HttpContext httpContext = mock(HttpContext.class);
        try {
            dispatcher.dispatch(new Object(), httpContext);
            fail();
        } catch(Exception ex) {
            verify(timer).time();
            verify(context).stop();
            verify(exceptionMeter).mark();
        }

    }

    private class TestResource extends AbstractResource {
        public TestResource(Class<?> resourceClass) {
            super(resourceClass);
        }

        public void defaultInstrumented() {

        }

        @NonInstrumented("Good reason for excluding this method")
        public void notInstrumented() {

        }
    }
}
