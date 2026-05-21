package com.shikou.hannime.util;


import com.shikou.model.entities.DownloadInfo;
import com.shikou.model.entities.DownloadItem;
import com.shikou.model.entities.HanimeVideo;
import com.shikou.model.entities.VideoQuality;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HanimeVideoUtils {

    public static VideoQuality getQualityUrl(HanimeVideo videoDetail, String quality){
        Map<String, VideoQuality> videoUrls = videoDetail.getVideoUrls();
        return videoUrls.getOrDefault(quality, null);
    }

    /**
     * 根据质量配置解析视频画质，直接从视频自身的videoUrls中按分辨率数值选取
     * @param downloadInfo 下载信息
     * @param qualityConfig 质量配置: "480p"=指定画质, "highest"=最高画质, "lowest"=最低画质
     * @return 匹配的VideoQuality，未找到返回null
     */
    public static VideoQuality resolveQuality(DownloadInfo downloadInfo, String qualityConfig) {
        Map<String, VideoQuality> videoUrls = downloadInfo.getDownloadItems().stream()
                .collect(Collectors.toMap(DownloadItem::getQuality, item -> {
                    VideoQuality quality = new VideoQuality();
                    quality.setQuality(item.getQuality());
                    quality.setSuffix(item.getItemType());
                    quality.setUrl(item.getDownloadUrl());
                    return quality;
                }));
        if (videoUrls.isEmpty()) {
            return null;
        }

        if ("highest".equalsIgnoreCase(qualityConfig)) {
            return videoUrls.entrySet().stream()
                    .max(Comparator.comparingInt(e -> extractResolution(e.getKey())))
                    .map(Map.Entry::getValue)
                    .orElse(null);
        } else if ("lowest".equalsIgnoreCase(qualityConfig)) {
            return videoUrls.entrySet().stream()
                    .min(Comparator.comparingInt(e -> extractResolution(e.getKey())))
                    .map(Map.Entry::getValue)
                    .orElse(null);
        } else {
            return videoUrls.get(qualityConfig.toUpperCase());
        }
    }

    /**
     * 从画质字符串中提取分辨率数值，如 "1080p" → 1080, "720p" → 720
     */
    private static int extractResolution(String quality) {
        String num = quality.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 根据请求的画质，从视频自身可用的videoUrls中匹配最接近的画质
     * <p>匹配不到时优先返回更高画质，其次更低画质
     */
    public static VideoQuality matchQualityUrl(HanimeVideo videoDetail, String quality){
        Map<String, VideoQuality> videoUrls = videoDetail.getVideoUrls();
        if (videoUrls.isEmpty()) {
            return null;
        }

        // 1. 先尝试精确匹配（大小写不敏感，videoUrls的key中P为大写）
        for (Map.Entry<String, VideoQuality> e : videoUrls.entrySet()) {
            if (e.getKey().equalsIgnoreCase(quality)) {
                return e.getValue();
            }
        }

        // 2. 从实际可用的画质中找最接近的
        int targetRes = extractResolution(quality);
        var sorted = videoUrls.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> extractResolution(e.getKey())))
                .toList();

        // 优先返回更高的画质
        for (var entry : sorted) {
            if (extractResolution(entry.getKey()) > targetRes) {
                return entry.getValue();
            }
        }

        // 其次返回更低的画质
        for (int i = sorted.size() - 1; i >= 0; i--) {
            if (extractResolution(sorted.get(i).getKey()) < targetRes) {
                return sorted.get(i).getValue();
            }
        }

        // 兜底：返回第一个可用画质
        return videoUrls.values().iterator().next();
    }

    /**
     * 从视频路径映射中按画质匹配最接近的路径
     * <p>匹配不到时优先返回更高画质，其次更低画质
     */
    public static String getQualityPath(Map<String, String> videoPaths, String quality){
        if (videoPaths.isEmpty()) {
            return null;
        }

        // 1. 先尝试精确匹配（大小写不敏感）
        for (Map.Entry<String, String> e : videoPaths.entrySet()) {
            if (e.getKey().equalsIgnoreCase(quality)) {
                return e.getValue();
            }
        }

        // 2. 从实际可用的画质中找最接近的
        int targetRes = extractResolution(quality);
        var sorted = videoPaths.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> extractResolution(e.getKey())))
                .toList();

        // 优先返回更高的画质
        for (var entry : sorted) {
            if (extractResolution(entry.getKey()) > targetRes) {
                return entry.getValue();
            }
        }

        // 其次返回更低的画质
        for (int i = sorted.size() - 1; i >= 0; i--) {
            if (extractResolution(sorted.get(i).getKey()) < targetRes) {
                return sorted.get(i).getValue();
            }
        }

        // 兜底：返回第一个可用画质
        return videoPaths.values().iterator().next();
    }
}
