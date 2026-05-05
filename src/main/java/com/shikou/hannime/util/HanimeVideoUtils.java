package com.shikou.hannime.util;

import com.shikou.model.HanimeVideo;
import com.shikou.model.VideoQuality;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HanimeVideoUtils {

    public static VideoQuality getResolutionUrl(HanimeVideo videoDetail, String resolution){
        Map<String, VideoQuality> videoUrls = videoDetail.getVideoUrls();
        return videoUrls.getOrDefault(resolution, null);
    }

    /**
     * 根据请求的画质，匹配最接近的画质URL
     * 匹配不到resolution时，返回更好一点的可用画质
     * @param videoDetail
     * @param resolution
     * @return
     */
    public static VideoQuality matchResolutionUrl(HanimeVideo videoDetail, String resolution){
        Map<String, VideoQuality> videoUrls = videoDetail.getVideoUrls();
        // 先尝试精确匹配
        VideoQuality videoQuality = videoUrls.get(resolution);
        if(videoQuality != null){
            return videoQuality;
        }

        // 定义画质优先级（从高到低）
        List<String> resolutionPriorities = Arrays.asList(
                "1080p", "720p", "480p", "360p", "240p", "144p"
        );

        // 尝试更高画质（如果请求的是720p，尝试1080p）
        int currentIndex = resolutionPriorities.indexOf(resolution.toLowerCase());
        if(currentIndex != -1){
            // 尝试更高画质
            for(int i = currentIndex - 1; i >= 0; i--){
                String higherRes = resolutionPriorities.get(i);
                videoQuality = videoUrls.get(higherRes);
                if(videoQuality != null){
                    return videoQuality;
                }
            }
            // 尝试更低画质
            for(int i = currentIndex + 1; i < resolutionPriorities.size(); i++){
                String lowerRes = resolutionPriorities.get(i);
                videoQuality = videoUrls.get(lowerRes);
                if(videoQuality != null){
                    return videoQuality;
                }
            }
        }

        // 如果没有找到任何画质，返回第一个可用的画质
        if(!videoUrls.isEmpty()){
            return videoUrls.values().iterator().next();
        }

        return null;
    }

    public static String getResolutionPath(Map<String, String> videoPaths, String resolution){
        // 先尝试精确匹配
        String videoPath = videoPaths.get(resolution);
        if(videoPath != null){
            return videoPath;
        }

        // 定义画质优先级（从高到低）
        List<String> resolutionPriorities = Arrays.asList(
                "1080p", "720p", "480p", "360p", "240p", "144p"
        );

        // 尝试更高画质（如果请求的是720p，尝试1080p）
        int currentIndex = resolutionPriorities.indexOf(resolution.toLowerCase());
        if(currentIndex != -1){
            // 尝试更高画质
            for(int i = currentIndex - 1; i >= 0; i--){
                String higherRes = resolutionPriorities.get(i);
                String higherResPath = videoPaths.get(higherRes);
                if(higherResPath != null){
                    return higherResPath;
                }
            }
            // 尝试更低画质
            for(int i = currentIndex + 1; i < resolutionPriorities.size(); i++){
                String lowerRes = resolutionPriorities.get(i);
                String lowerResPath = videoPaths.get(lowerRes);
                if(lowerResPath != null){
                    return lowerResPath;
                }
            }
        }

        // 如果没有找到任何画质，返回第一个可用的画质
        if(!videoPaths.isEmpty()){
            return videoPaths.values().iterator().next();
        }

        return null;
    }
}
