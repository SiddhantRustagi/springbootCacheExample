package com.redis.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.stereotype.Component;

@Component("shardCacheResolver")
public class ShardCacheResolver implements CacheResolver {

    private final CacheManager shard0CacheManager;
    private final CacheManager shard1CacheManager;

    private final Map<String, MaxKeysCache> wrappedCaches = new ConcurrentHashMap<>();

    public ShardCacheResolver(
            @Qualifier("shard0CacheManager") CacheManager shard0CacheManager,
            @Qualifier("shard1CacheManager") CacheManager shard1CacheManager) {
        this.shard0CacheManager = shard0CacheManager;
        this.shard1CacheManager = shard1CacheManager;
    }

}
