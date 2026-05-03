package com.shikou.hannime.api;

import com.shikou.hannime.config.VideoStoreConfig;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.entities.request.CreateTaskRequest;
import com.shikou.hannime.entities.response.Result;
import com.shikou.hannime.service.VideoDownloadTaskService;
import com.shikou.hannime.service.VideoService;
import com.shikou.hannime.util.AssertUtils;
import jakarta.annotation.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/task")
public class VideoDownloadTaskAPI {
    @Resource
    private VideoService videoService;
    @Resource
    private VideoDownloadTaskService videoDownloadTaskService;

    @Resource
    private VideoStoreConfig videoStoreConfig;

    @PostMapping("/create")
    public Result createTask(CreateTaskRequest request) {
        AssertUtils.nonNull(request, "请求参数不能为空");
        List<CreateTaskRequest.Task> tasks = request.getTasks();
        AssertUtils.isNotEmpty(tasks, "任务列表不能为空");

        List<VideoDownloadTask> downloadTasks = new ArrayList<>();

        for (CreateTaskRequest.Task task : tasks) {
            String videoCode = task.getVideoCode();
            AssertUtils.isNotBlank(videoCode, "视频代码不能为空");
            List<String> resolutions = task.getResolutions();
            if (CollectionUtils.isEmpty(resolutions)){
                VideoDownloadTask downloadTask = new VideoDownloadTask();
                downloadTask.setVideoCode(videoCode);
                downloadTask.setVideoUrl(task.getVideoUrl());
                downloadTask.setResolution(videoStoreConfig.getResolution());
                downloadTasks.add(downloadTask);
            }else{
                for (String resolution : resolutions){
                    VideoDownloadTask downloadTask = new VideoDownloadTask();
                    downloadTask.setVideoCode(videoCode);
                    downloadTask.setVideoUrl(task.getVideoUrl());
                    downloadTask.setResolution(resolution);
                    downloadTasks.add(downloadTask);
                }
            }
        }
        videoDownloadTaskService.createTasks(downloadTasks);
        return Result.success();
    }
}
