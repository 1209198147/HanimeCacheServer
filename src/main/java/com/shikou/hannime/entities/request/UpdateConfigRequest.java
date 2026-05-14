package com.shikou.hannime.entities.request;

import lombok.Data;

@Data
public class UpdateConfigRequest {
    /** 视频存储路径 */
    private String videoStorePath;
    /** 默认画质 */
    private String quality;
}
