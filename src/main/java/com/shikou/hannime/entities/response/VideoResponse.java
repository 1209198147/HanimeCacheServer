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

    /** 标题 */
    private String title;

    /** 标题 */
    private String chineseTitle;

    /** 封面/缩略图URL */
    private String coverUrl;

    /** 简介 */
    private String introduction;

    /** 标签列表 */
    private List<String> tags;

    /** 时长，如 "11:00" */
    private String duration;

    /** 上传时间 */
    private String uploadTime;

    /** 上传者名称，如 "紺そめ（Consome）" */
    private String uploader;

    /** 类型 */
    private String genre;

    public static VideoResponse fromVideo(Video video){
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setVideoCode(video.getVideoCode());
        videoResponse.setTitle(video.getTitle());
        videoResponse.setChineseTitle(video.getTitle());
        videoResponse.setCoverUrl(video.getCoverUrl());
        videoResponse.setIntroduction(video.getIntroduction());
        videoResponse.setTags(video.getTagsAsList());
        videoResponse.setUploadTime(video.getUploadTime());
        videoResponse.setUploader(video.getUploader());
        videoResponse.setGenre(video.getGenre());
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
