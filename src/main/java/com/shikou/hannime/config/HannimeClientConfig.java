package com.shikou.hannime.config;

import com.shikou.client.HanimeApiClient;
import com.shikou.config.HanimeConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HannimeClientConfig {
    /** 自定义Base URL */
    @Value("${hannime.zone:HK}")
    private String zone;

    @Bean
    public HanimeApiClient hanimeApiClient() {
        if ("HK".equalsIgnoreCase(zone)) {
            return HanimeApiClient.builder()
                    .config(HanimeConfig.defaultConfigHK())
                    .build();
        }
        return HanimeApiClient.builder()
                .config(HanimeConfig.defaultConfig())
                .build();
    }
}
