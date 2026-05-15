package com.shikou.hannime.task;

import com.shikou.client.HanimeApiClient;
import com.shikou.hannime.manager.CacheManager;
import com.shikou.hannime.service.ProxyService;
import com.shikou.model.entities.HomePageSection;
import com.shikou.model.entities.VideoInfo;
import com.shikou.model.entities.pages.HomePage;
import com.shikou.model.entities.pages.WatchPage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(value = "cache.task.enable", havingValue = "true")
public class CacheTaskScheduler {
    @Resource
    private CacheManager cacheManager;
    @Resource
    private ProxyService proxyService;
    @Resource
    private HanimeApiClient hanimeApiClient;

    @Scheduled(fixedDelay = 900000)
    private void cacheHomePageTask() {
        log.debug("开始缓存首页内容");
        HomePage homePage = proxyService.getHomePage();
        List<HomePageSection> sections = homePage.getSections();
        for (HomePageSection section : sections) {
            List<String> videoCodes = section.getVideoInfoList().stream()
                    .map(VideoInfo::getVideoCode)
                    .filter(StringUtils::isNotEmpty)
                    .toList();
            for (String videoCode : videoCodes) {
                try {
                    WatchPage watchPage = hanimeApiClient.getWatchPage(videoCode);
                    String key = CacheManager.getKey(CacheManager.WATCH_KEY_PREFIX, videoCode);
                    cacheManager.put(key, watchPage, Duration.ofMillis(900000));
                } catch (Exception e) {
                    log.debug("缓存首页内容 videoCode: {} 失败", videoCode, e);
                }
            }
        }
        log.debug("缓存首页内容完成");

    }
}
