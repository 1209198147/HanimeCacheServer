package com.shikou.hannime.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class VideoStoreConfig {
    @Value("${video.store.path}")
    private String videoStorePath;
    @Value("${video.min.code:10}")
    private Integer minVideoCode;
    @Value("${video.max.code:1000000}")
    private Integer maxVideoCode;
    @Value("${video.resolution:360p}")
    private String resolution;
}
