package com.shikou.hannime.service;

import com.shikou.hannime.config.VideoStoreConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务配置管理服务，支持运行时读取与更新配置。
 * 初始值从 application.properties 加载，后续可通过 API 动态修改。
 */
@Slf4j
@Data
@Service
public class TaskConfigService {

    @Resource
    private VideoStoreConfig videoStoreConfig;

    /** 视频存储路径 */
    private volatile String videoStorePath;
    /** 默认画质 */
    private volatile String quality;

    @PostConstruct
    private void init() {
        this.videoStorePath = videoStoreConfig.getVideoStorePath();
        this.quality = videoStoreConfig.getQuality();
        log.info("任务配置初始化完成 path={} quality={}",
                videoStorePath, quality);
    }

    /**
     * 获取所有配置项
     */
    public Map<String, Object> getAllConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("videoStorePath", videoStorePath);
        config.put("quality", quality);
        return config;
    }

    /**
     * 更新配置项（只更新传入的非空字段）
     */
    public void updateConfig(String videoStorePath, String quality) {
        if (StringUtils.isNotBlank(videoStorePath)) {
            this.videoStorePath = videoStorePath;
            log.info("配置更新 videoStorePath={}", videoStorePath);
        }
        if (StringUtils.isNotBlank(quality)) {
            this.quality = quality;
            log.info("配置更新 quality={}", quality);
        }
    }
}
