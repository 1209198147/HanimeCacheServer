package com.shikou.hannime.api;

import com.shikou.hannime.entities.response.Result;
import com.shikou.hannime.service.VideoService;
import com.shikou.hannime.util.AssertUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video")
public class VideoAPI {
    @Resource
    private VideoService videoService;

    @GetMapping("/{videCode}")
    public Result getVideo(@PathVariable("videCode") String videoCode) {
        AssertUtils.isNotEmpty(videoCode, "视频码不能为空");
        return Result.success(videoService.getVideo(videoCode));
    }
}
