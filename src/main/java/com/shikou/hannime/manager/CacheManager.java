package com.shikou.hannime.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.shikou.model.entities.pages.HomePage;
import com.shikou.model.entities.pages.UserPage;
import com.shikou.model.entities.pages.WatchPage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 缓存管理器
 */
@Slf4j
@Component
public class CacheManager {

    public static final String HOME_KEY = "hanime:home";
    public static final String WATCH_KEY_PREFIX = "hanime:watch";
    public static final String USER_KEY_PREFIX = "hanime:user";
    
    @Autowired(required = false)
    private Cache<String, CacheEntity> cache;

    public Object getIfPresent(String key){
        CacheEntity entity = cache.getIfPresent(key);
        return entity == null ? null : entity.getData();
    }

    public void put(String key, Object data){
        put(key, data, null);
    }

    public void put(String key, Object data, Duration duration){
        cache.put(key, CacheEntity.of(data, duration));
    }


    public HomePage getHome() {
        if(cache == null) return null;
        HomePage data = (HomePage) getIfPresent(HOME_KEY);
        if (data != null) {
            log.debug("缓存命中 home");
        }
        return  data;
    }

    public void putHome(HomePage data) {
        if(cache == null) return;
        if (data != null) {
            cache.put(HOME_KEY, CacheEntity.of(data));
            log.debug("缓存写入 home");
        }
    }


    public WatchPage getWatch(String videoCode) {
        if(cache == null) return null;
        String key = getKey(WATCH_KEY_PREFIX, videoCode);
        WatchPage data = (WatchPage) getIfPresent(key);
        if (data != null) {
            log.debug("缓存命中 watch:{}", videoCode);
        }
        return data;
    }

    public void putWatch(String videoCode, WatchPage data) {
        if(cache == null) return;
        if (data != null) {
            cache.put(getKey(WATCH_KEY_PREFIX, videoCode), CacheEntity.of(data));
            log.debug("缓存写入 watch:{}", videoCode);
        }
    }

    public UserPage getUser(String userId) {
        if(cache == null) return null;
        String key = getKey(USER_KEY_PREFIX, userId);
        UserPage data = (UserPage) getIfPresent(key);
        if (data != null) {
            log.debug("缓存命中 user:{}", userId);
        }
        return data;
    }

    public void putUser(String userId, UserPage data) {
        if(cache == null) return;
        if (data != null) {
            cache.put(getKey(USER_KEY_PREFIX, userId), CacheEntity.of(data));
            log.debug("缓存写入 user:{}", userId);
        }
    }

    public static String getKey(String... parts){
        return String.join(":", parts);
    }

    @Data
    public static class CacheEntity {
        private Object data;
        private boolean needRestExpireTime;
        private Duration duration;

        public CacheEntity(Object data, boolean isFresh, Duration duration) {
            this.data = data;
            this.needRestExpireTime = isFresh;
            this.duration = duration;
        }

        public static CacheEntity of(Object data) {
            return of(data, false, null);
        }

        public static CacheEntity of(Object data, Duration duration) {
            return of(data, false, duration);
        }

        public static CacheEntity of(Object data, boolean isFresh, Duration duration) {
            return new CacheEntity(data, isFresh, duration);
        }
    }
}