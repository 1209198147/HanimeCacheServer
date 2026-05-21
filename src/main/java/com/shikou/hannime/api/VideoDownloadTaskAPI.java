package com.shikou.hannime.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.entities.enums.EngineStatus;
import com.shikou.hannime.entities.enums.TaskStatus;
import com.shikou.hannime.entities.request.CreateTaskRequest;
import com.shikou.hannime.entities.request.TaskOperationRequest;
import com.shikou.hannime.entities.response.Result;
import com.shikou.hannime.exception.ErrorCode;
import com.shikou.hannime.service.TaskConfigService;
import com.shikou.hannime.service.VideoDownloadTaskService;
import com.shikou.hannime.service.VideoService;
import com.shikou.hannime.task.TaskEngine;
import com.shikou.hannime.util.AssertUtils;
import jakarta.annotation.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/task")
public class VideoDownloadTaskAPI {
    @Resource
    private VideoService videoService;
    @Resource
    private VideoDownloadTaskService videoDownloadTaskService;

    @Resource
    private TaskConfigService taskConfigService;
    @Resource
    private TaskEngine taskEngine;

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
        // 为每个任务计算进度百分比
        List<Map<String, Object>> enrichedList = result.getRecords().stream().map(task -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", task.getId());
            map.put("videoCode", task.getVideoCode());
            map.put("status", task.getStatus());
            map.put("quality", task.getQuality());
            map.put("currentBytes", task.getCurrentBytes());
            map.put("totalBytes", task.getTotalBytes());
            long currentBytes = task.getCurrentBytes() != null ? task.getCurrentBytes() : 0;
            long totalBytes = task.getTotalBytes() != null ? task.getTotalBytes() : 0;
            int progress = totalBytes > 0 ? (int) (currentBytes * 100 / totalBytes) : 0;
            map.put("progress", progress);
            map.put("errorMessage", task.getErrorMessage());
            map.put("createTime", task.getCreateTime());
            map.put("updateTime", task.getUpdateTime());
            return map;
        }).collect(Collectors.toList());
        Map<String, Object> data = Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "list", enrichedList
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
        taskEngine.cancelTask(request.getTaskId());
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

    // ==================== 新增端点 ====================

    /**
     * 暂停单个任务
     */
    @PostMapping("/pause")
    public Result pauseTask(@RequestBody TaskOperationRequest request) {
        AssertUtils.nonNull(request, "请求参数不能为空");
        AssertUtils.nonNull(request.getTaskId(), "任务ID不能为空");
        taskEngine.pauseTask(request.getTaskId());
        return Result.success();
    }

    /**
     * 继续单个任务
     */
    @PostMapping("/resume")
    public Result resumeTask(@RequestBody TaskOperationRequest request) {
        AssertUtils.nonNull(request, "请求参数不能为空");
        AssertUtils.nonNull(request.getTaskId(), "任务ID不能为空");
        taskEngine.resumeTask(request.getTaskId());
        return Result.success();
    }

    /**
     * 暂停所有任务
     */
    @PostMapping("/pauseAll")
    public Result pauseAll() {
        if (taskEngine.getEngineStatus() == EngineStatus.STOPPED) {
            return Result.error(ErrorCode.TASK_ENGINE_STOPPED, "引擎已停止，无法暂停");
        }
        int count = taskEngine.pauseAll();
        return Result.success(Map.of("pausedCount", count));
    }

    /**
     * 恢复所有任务
     */
    @PostMapping("/resumeAll")
    public Result resumeAll() {
        if (taskEngine.getEngineStatus() == EngineStatus.STOPPED) {
            taskEngine.start();
        }
        int count = taskEngine.resumeAll();
        return Result.success(Map.of("resumedCount", count));
    }

    /**
     * 停止所有任务
     */
    @PostMapping("/stopAll")
    public Result stopAll() {
        int count = taskEngine.stopAll();
        return Result.success(Map.of("stoppedCount", count));
    }

    /**
     * 查询引擎状态
     */
    @GetMapping("/engine/status")
    public Result getEngineStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("engineStatus", taskEngine.getEngineStatus().getMessage());
        status.put("runningCount", taskEngine.getRunningCount());
        // 查询 DB 中 paused 数量
        Map<String, Long> stats = videoDownloadTaskService.getStats();
        status.put("pausedCount", stats.getOrDefault("paused", 0L));
        status.put("runningTaskIds", taskEngine.getRunningTaskIds());
        return Result.success(status);
    }
}
