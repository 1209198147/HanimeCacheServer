package com.shikou.hannime.entities.response;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.shikou.hannime.entities.domain.Video;
import lombok.Data;

import java.util.List;

@Data
public class VideoResponse {
    /** 影片代码，如 "403047" */
    private String videoCode;

    /** 影片观看URL，如果本地有缓存会返回本地缓存的URL */
    private String videoUrl;

    /** 标题 */
    private String title;

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
        videoResponse.setVideoUrl(video.getVideoUrl());
        videoResponse.setTitle(video.getTitle());
        videoResponse.setCoverUrl(video.getCoverUrl());
        videoResponse.setIntroduction(video.getIntroduction());
        videoResponse.setTags(video.getTagsAsList());
        videoResponse.setUploadTime(video.getUploadTime());
        videoResponse.setUploader(video.getUploader());
        videoResponse.setGenre(video.getGenre());
        return videoResponse;
    }
}
