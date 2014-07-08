package com.yammer.dropwizard.auth;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;

/**
 * An {@link Authenticator} decorator which uses a Guava cache to temporarily cache credentials and
 * their corresponding principals.
 *
 * @param <C>    the type of credentials the authenticator can authenticate
 * @param <P>    the type of principals the authenticator returns
 */
public class CachingAuthenticator<C, P> implements Authenticator<C, P> {

    /**
     * Wraps an underlying authenticator with a cache.
     *
     * @param authenticator the underlying authenticator
     * @param cacheSpec     a {@link CacheBuilderSpec}
     * @param <C>           the type of credentials the authenticator can authenticate
     * @param <P>           the type of principals the authenticator returns
     * @return a cached version of {@code authenticator}
     */
    public static <C, P> CachingAuthenticator<C, P> wrap(Authenticator<C, P> authenticator,
                                                         CacheBuilderSpec cacheSpec) {
        return new CachingAuthenticator<C, P>(authenticator, CacheBuilder.from(cacheSpec));
    }
    
    private final Authenticator<C, P> underlying;
    private final LoadingCache<C, Optional<P>> cache;

    private CachingAuthenticator(Authenticator<C, P> authenticator,
                                 CacheBuilder<Object, Object> builder) {
        this.underlying = authenticator;
        this.cache = builder.recordStats().build(new CacheLoader<C, Optional<P>>() {
            @Override
            public Optional<P> load(C key) throws Exception {
                return underlying.authenticate(key);
            }
        });
    }

    @Override
    public Optional<P> authenticate(C credentials) throws AuthenticationException {
        try {
            return cache.get(credentials);
        } catch (ExecutionException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * Discards any cached principal for the given credentials.
     *
     * @param credentials    a set of credentials
     */
    public void invalidate(C credentials) {
        cache.invalidate(credentials);
    }

    /**
     * Discards any cached principal for the given collection of credentials.
     *
     * @param credentials    a collection of credentials
     */
    public void invalidateAll(Iterable<C> credentials) {
        cache.invalidateAll(credentials);
    }

    /**
     * Discards all cached principals.
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * Returns the number of cached principals.
     *
     * @return the number of cached principals
     */
    public long size() {
        return cache.size();
    }

    /**
     * Returns a set of statistics about the cache contents and usage.
     *
     * @return a set of statistics about the cache contents and usage
     */
    public CacheStats stats() {
        return cache.stats();
    }
}
