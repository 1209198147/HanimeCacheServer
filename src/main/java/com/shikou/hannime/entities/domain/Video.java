package com.shikou.hannime.entities.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@TableName("video")
@Data
public class Video {
    /** ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 影片代码，如 "403047" */
    private String videoCode;

    /** 影片观看URL，如 "https://hanimeone.me/watch?v=403047" */
    private String videoUrl;

    /** 分辨率标识，如 "1080P", "720P", "480P", "240P", "Unknown" */
    private String resolution;

    /** 标题 */
    private String title;

    /** 封面/缩略图URL */
    private String coverUrl;

    /** 简介 */
    private String introduction;

    /** 标签列表（以逗号分隔的字符串） */
    private String tags;

    /** 上传时间 */
    private String uploadTime;

    /** 上传者名称，如 "紺そめ（Consome）" */
    private String uploader;

    /** 类型 */
    private String genre;

    /** 路径 */
    private String path;

    /**
     * 获取标签列表
     * @return 标签列表
     */
    public List<String> getTagsAsList() {
        if (tags == null || tags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 设置标签列表
     * @param tagsList 标签列表
     */
    public void setTagsFromList(List<String> tagsList) {
        if (tagsList == null || tagsList.isEmpty()) {
            this.tags = "";
        } else {
            this.tags = tagsList.stream()
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.joining(","));
        }
    }
}
