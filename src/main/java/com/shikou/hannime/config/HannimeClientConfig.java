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
    @Value("${hannime.lang:zhs}")
    private String lang;

    @Bean
    public HanimeApiClient hanimeApiClient() {
        HanimeConfig config = null;
        if ("HK".equalsIgnoreCase(zone)) {
            config = HanimeConfig.builder()
                    .baseUrl(HanimeConfig.DEFAULT_BASE_URL_HK)
                    .userLang(lang)
                    .build();
        }else{
            config = HanimeConfig.builder()
                    .baseUrl(HanimeConfig.DEFAULT_BASE_URL)
                    .userLang(lang)
                    .build();
        }
        return HanimeApiClient.builder().config(config).build();
    }
}
