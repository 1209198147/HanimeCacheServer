package com.shikou.hannime.entities.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("video_download_task")
public class VideoDownloadTask {
    /** ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String videoCode;
    private Integer status;
    /** 分辨率标识，如 "1080P", "720P", "480P", "240P", "Unknown" */
    private String resolution;

    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
