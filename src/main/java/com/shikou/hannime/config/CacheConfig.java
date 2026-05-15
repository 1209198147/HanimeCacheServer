package com.shikou.hannime.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.shikou.hannime.manager.CacheManager;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 缓存配置
 */
@Configuration
public class CacheConfig {
    @Value("${cache.duration:1}")
    private long duration = 1;
    @Value("${cache.unit:MINUTES}")
    private TimeUnit unit = TimeUnit.MINUTES;
    @Value("${cache.maxSize:1000}")
    private int maxSize = 1000;

    /**
     * 代理数据缓存
     */
    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public Cache<String, CacheManager.CacheEntity> cache() {
        return Caffeine.newBuilder()
                .expireAfter(new Expiry<String, CacheManager.CacheEntity>() {
                    @Override
                    public long expireAfterCreate(@NonNull String s, CacheManager.@NonNull CacheEntity caffeineEntry, long l) {
                        Duration entryDuration = caffeineEntry.getDuration();
                        if (entryDuration != null){
                            return entryDuration.toNanos();
                        }
                        return unit.toNanos(duration);
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull String s, CacheManager.@NonNull CacheEntity caffeineEntry, long l, @NonNegative long l1) {
                        Duration entryDuration = caffeineEntry.getDuration();
                        if (entryDuration != null){
                            return entryDuration.toNanos();
                        }
                        return unit.toNanos(duration);
                    }

                    @Override
                    public long expireAfterRead(@NonNull String s, CacheManager.@NonNull CacheEntity caffeineEntry, long l, @NonNegative long l1) {
                        if (caffeineEntry.isNeedRestExpireTime()) {
                            Duration entryDuration = caffeineEntry.getDuration();
                            if (entryDuration != null){
                                return entryDuration.toNanos();
                            }
                            return unit.toNanos(duration);
                        }
                        return l1;
                    }
                })
                .maximumSize(maxSize)
                .build();
    }
}
