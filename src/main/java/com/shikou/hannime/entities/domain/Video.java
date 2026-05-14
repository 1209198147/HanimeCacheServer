package com.shikou.hannime.entities.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("video")
@Data
public class Video {
    /** ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 影片代码，如 "403047" */
    private String videoCode;

    /** 画质标识，如 "1080P", "720P", "480P", "240P" */
    private String quality;

    /** 本地文件路径（NULL=未下载） */
    private String path;
}
