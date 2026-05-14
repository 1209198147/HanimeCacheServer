package com.shikou.hannime.entities.response;

import com.shikou.hannime.entities.domain.Video;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VideoResponse {
    /** 影片代码，如 "403047" */
    private String videoCode;

    /** 多分辨率视频URL映射: 分辨率 -> VideoQuality */
    private Map<String, HanimeLink> videoUrls;

    public static VideoResponse fromVideo(Video video){
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setVideoCode(video.getVideoCode());
        return videoResponse;
    }

    @Data
    @Builder
    public static class HanimeLink {
        /**
         * 链接
         */
        private String link;
        /**
         * 子类型
         */
        private String subtype;
        /**
         * 后缀
         */
        private String suffix;
    }
}
