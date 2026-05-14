package com.shikou.hannime.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class VideoStoreConfig {
    @Value("${video.store.path}")
    private String videoStorePath;
    @Value("${video.quality:360p}")
    private String quality;
}
