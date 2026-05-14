package com.shikou.hannime.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.entities.enums.TaskStatus;
import com.shikou.hannime.entities.request.CreateTaskRequest;
import com.shikou.hannime.entities.request.TaskOperationRequest;
import com.shikou.hannime.entities.response.Result;
import com.shikou.hannime.service.TaskConfigService;
import com.shikou.hannime.service.VideoDownloadTaskService;
import com.shikou.hannime.service.VideoService;
import com.shikou.hannime.util.AssertUtils;
import jakarta.annotation.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/task")
public class VideoDownloadTaskAPI {
    @Resource
    private VideoService videoService;
    @Resource
    private VideoDownloadTaskService videoDownloadTaskService;

    @Resource
    private TaskConfigService taskConfigService;

    @PostMapping("/create")
    public Result createTask(@RequestBody CreateTaskRequest request) {
        AssertUtils.nonNull(request, "请求参数不能为空");
        List<CreateTaskRequest.Task> tasks = request.getTasks();
        AssertUtils.isNotEmpty(tasks, "任务列表不能为空");

        List<VideoDownloadTask> downloadTasks = new ArrayList<>();

        for (CreateTaskRequest.Task task : tasks) {
            String videoCode = task.getVideoCode();
            AssertUtils.isNotBlank(videoCode, "视频代码不能为空");
            List<String> qualities = task.getQualities();
            if (CollectionUtils.isEmpty(qualities)){
                VideoDownloadTask downloadTask = new VideoDownloadTask();
                downloadTask.setVideoCode(videoCode);
                downloadTask.setQuality(taskConfigService.getQuality());
                downloadTasks.add(downloadTask);
            }else{
                for (String quality : qualities){
                    VideoDownloadTask downloadTask = new VideoDownloadTask();
                    downloadTask.setVideoCode(videoCode);
                    downloadTask.setQuality(quality);
                    downloadTasks.add(downloadTask);
                }
            }
        }
        videoDownloadTaskService.createTasks(downloadTasks);
        return Result.success();
    }

    /**
     * 分页查询任务列表
     * @param videoCode 视频码（可选）
     * @param status 任务状态（可选）
     * @param page 页码（默认1）
     * @param size 每页数量（默认20）
     */
    @GetMapping("/list")
    public Result listTasks(@RequestParam(required = false) String videoCode,
                            @RequestParam(required = false) Integer status,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size) {
        AssertUtils.isTrue(page < 1, "页码不能小于1");
        AssertUtils.isTrue(size < 1 || size > 100, "每页数量范围为1-100");
        Page<VideoDownloadTask> result = videoDownloadTaskService.listTasks(videoCode, status, page, size);
        Map<String, Object> data = Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "list", result.getRecords()
        );
        return Result.success(data);
    }

    /**
     * 获取任务统计信息
     */
    @GetMapping("/stats")
    public Result getStats() {
        Map<String, Long> stats = videoDownloadTaskService.getStats();
        return Result.success(stats);
    }

    /**
     * 取消任务
     */
    @PostMapping("/cancel")
    public Result cancelTask(@RequestBody TaskOperationRequest request) {
        AssertUtils.nonNull(request, "请求参数不能为空");
        AssertUtils.nonNull(request.getTaskId(), "任务ID不能为空");
        VideoDownloadTask task = new VideoDownloadTask();
        task.setId(request.getTaskId());
        videoDownloadTaskService.cancelTask(task);
        return Result.success();
    }

    /**
     * 重试失败的任务
     */
    @PostMapping("/retry")
    public Result retryTask(@RequestBody TaskOperationRequest request) {
        AssertUtils.nonNull(request, "请求参数不能为空");
        AssertUtils.nonNull(request.getTaskId(), "任务ID不能为空");
        videoDownloadTaskService.retryTask(request.getTaskId());
        return Result.success();
    }
}
