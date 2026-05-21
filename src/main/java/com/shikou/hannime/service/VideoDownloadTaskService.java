package com.shikou.hannime.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shikou.hannime.dao.VideoDownloadTaskMapper;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.entities.enums.TaskStatus;
import com.shikou.hannime.exception.ErrorCode;
import com.shikou.hannime.util.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VideoDownloadTaskService extends ServiceImpl<VideoDownloadTaskMapper, VideoDownloadTask> {
    private final Set<Integer> NOT_COMPLETED_TASK_STATUS = Set.of(
            TaskStatus.PENDING.getCode(),
            TaskStatus.FAILED.getCode(),
            TaskStatus.PAUSED.getCode()
    );

    public void createTask(VideoDownloadTask task){
        checkTask(task);
        task.setStatus(TaskStatus.PENDING.getCode());
        this.save(task);
    }

    private static void checkTask(VideoDownloadTask task) {
        AssertUtils.nonNull(task, "任务不能为空");
        AssertUtils.isNotEmpty(task.getVideoCode(), "视频码不能为空");
    }

    @Transactional(rollbackFor = Exception.class)
    public void createTasks(List<VideoDownloadTask> tasks){
        AssertUtils.nonNull(tasks, "任务不能为空");
        AssertUtils.isNotEmpty(tasks, "视频任务列表不能为空");
        int total = tasks.size();
        List<String> videoCodeList = tasks.stream().map(VideoDownloadTask::getVideoCode).collect(Collectors.toList());
        List<VideoDownloadTask> existTasks = this.lambdaQuery().in(VideoDownloadTask::getVideoCode, videoCodeList).in(VideoDownloadTask::getStatus, NOT_COMPLETED_TASK_STATUS).list();

        Set<String> existTasksSet = existTasks.stream()
                .map(task -> task.getVideoCode() + "_" + task.getQuality())
                .collect(Collectors.toSet());
        List<VideoDownloadTask> needCreate = tasks.stream()
                .filter(task -> !existTasksSet.contains(task.getVideoCode() + "_" + task.getQuality()))
                .map(task -> {
                    task.setStatus(TaskStatus.PENDING.getCode());
                    return task;
                }).collect(Collectors.toList());
        this.saveBatch(needCreate);
        log.info("创建任务完成，创建了 {} 个任务，共 {} 个任务已存在", needCreate.size(), total - needCreate.size());
    }

    public List<VideoDownloadTask> getNotCompletedTask(int count) {
        return this.query().in("status", NOT_COMPLETED_TASK_STATUS).last("LIMIT " + count).list();
    }

    public List<VideoDownloadTask> getRecoverTask() {
        return this.lambdaQuery()
                .eq(VideoDownloadTask::getStatus, TaskStatus.PROCESSING.getCode())
                .le(VideoDownloadTask::getUpdateTime, Date.from(Instant.now().minusSeconds(2*60*60)))
                .list();
    }

    public void processTask(VideoDownloadTask videoDownloadTask) {
        Integer id = videoDownloadTask.getId();
        String videoCode = videoDownloadTask.getVideoCode();
        VideoDownloadTask task = null;
        if (id != null){
            task = this.getById(id);
        }else if(videoCode != null){
            task = this.lambdaQuery().eq(VideoDownloadTask::getVideoCode, videoCode).one();
        }else{
            AssertUtils.fail(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在");

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.PAUSED)
                        .contains(taskStatus),
                "当前任务无法处理 id:" + id + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.PROCESSING.getCode());
        boolean result = this.updateById(task);
        if (!result){
            log.error("任务状态无法改为处理中 id:{} videoCode:{}", id, videoCode);
        }
        log.info("任务处理中 id:{} videoCode:{}", id, videoCode);
    }

    public void completeTask(VideoDownloadTask videoDownloadTask) {
        Integer id = videoDownloadTask.getId();
        String videoCode = videoDownloadTask.getVideoCode();
        VideoDownloadTask task = null;
        if (id != null){
            task = this.getById(id);

        }else if(videoCode != null){
            task = this.lambdaQuery().eq(VideoDownloadTask::getVideoCode, videoCode).one();
        }else{
            AssertUtils.fail(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在");

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(!TaskStatus.PROCESSING.equals(taskStatus),
                "当前任务无法完成 id:" + id + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.COMPLETED.getCode());
        boolean result = this.updateById(task);
        if (!result){
            log.error("任务完成失败 id:{} videoCode:{}", id, videoCode);
        }
        log.info("任务完成 id:{} videoCode:{}", id, videoCode);
    }

    public void failTask(VideoDownloadTask videoDownloadTask) {
        Integer id = videoDownloadTask.getId();
        String videoCode = videoDownloadTask.getVideoCode();
        VideoDownloadTask task = null;
        if (id != null){
            task = this.getById(id);
        }else if(videoCode != null){
            task = this.lambdaQuery().eq(VideoDownloadTask::getVideoCode, videoCode).one();
        }else{
            AssertUtils.fail(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在");

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(Set.of(TaskStatus.FAILED, TaskStatus.COMPLETED, TaskStatus.PENDING, TaskStatus.CANCELLED)
                        .contains(taskStatus),
                "当前任务无法失败 id:" + id + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.FAILED.getCode());
        boolean result = this.updateById(task);
        if (!result){
            log.error("任务无法失败 id:{} videoCode:{}", id, videoCode);
        }
        log.info("任务失败 id:{} videoCode:{}", id, videoCode);
    }

    public void cancelTask(VideoDownloadTask videoDownloadTask) {
        Integer id = videoDownloadTask.getId();
        String videoCode = videoDownloadTask.getVideoCode();
        VideoDownloadTask task = null;
        if (id != null){
            task = this.getById(id);
        }else if(videoCode != null){
            task = this.lambdaQuery().eq(VideoDownloadTask::getVideoCode, videoCode).one();
        }else{
            AssertUtils.fail(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在");

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(Set.of(TaskStatus.CANCELLED, TaskStatus.COMPLETED)
                        .contains(taskStatus),
                "当前任务无法取消 id:" + id + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.CANCELLED.getCode());
        boolean result = this.updateById(task);
        if (!result){
            log.error("任务无法取消 id:{} videoCode:{}", id, videoCode);
        }
        log.info("任务取消 id:{} videoCode:{}", id, videoCode);
    }

    /**
     * 分页查询任务列表，支持按状态和视频码过滤
     */
    public Page<VideoDownloadTask> listTasks(String videoCode, Integer status, int page, int size) {
        Page<VideoDownloadTask> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<VideoDownloadTask> wrapper = new LambdaQueryWrapper<>();
        if (videoCode != null && !videoCode.isBlank()) {
            wrapper.eq(VideoDownloadTask::getVideoCode, videoCode);
        }
        if (status != null) {
            wrapper.eq(VideoDownloadTask::getStatus, status);
        }
        wrapper.orderByDesc(VideoDownloadTask::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    /**
     * 获取任务统计信息（各状态数量），使用 SQL 聚合避免全量加载
     */
    public Map<String, Long> getStats() {
        List<Map<String, Object>> rows = this.getBaseMapper().countByStatus();
        Map<String, Long> stats = new LinkedHashMap<>();
        long total = 0;
        for (Map<String, Object> row : rows) {
            int status = ((Number) row.get("status")).intValue();
            long count = ((Number) row.get("cnt")).longValue();
            total += count;
            try {
                TaskStatus ts = TaskStatus.valueOf(status);
                stats.put(ts.getMessage().toLowerCase(), count);
            } catch (IllegalArgumentException e) {
                stats.put("unknown_" + status, count);
            }
        }
        stats.put("total", total);
        // 确保所有已知状态都有值
        for (TaskStatus ts : TaskStatus.values()) {
            stats.putIfAbsent(ts.getMessage().toLowerCase(), 0L);
        }
        return stats;
    }

    /**
     * 重试失败的任务，将状态重置为 PENDING
     */
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Integer taskId) {
        AssertUtils.nonNull(taskId, "任务ID不能为空");
        VideoDownloadTask task = this.getById(taskId);
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在 id:" + taskId);

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(!TaskStatus.FAILED.equals(taskStatus),
                "当前任务无法重试 id:" + taskId + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.PENDING.getCode());
        this.updateById(task);
        log.info("任务重试 id:{} videoCode:{}", taskId, task.getVideoCode());
    }

    // ==================== 新增方法 ====================

    /**
     * 更新单个任务状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatus(Integer taskId, Integer status) {
        VideoDownloadTask task = this.getById(taskId);
        if (task != null) {
            task.setStatus(status);
            this.updateById(task);
        }
    }

    /**
     * 批量更新指定状态的任务为目标状态
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateStatusByStatus(Integer targetStatus, Integer sourceStatus) {
        return this.getBaseMapper().updateStatusByStatus(targetStatus, sourceStatus);
    }

    /**
     * 批量更新指定状态列表的任务为目标状态
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateStatusByStatusList(Integer targetStatus, List<Integer> sourceStatusList) {
        return this.getBaseMapper().updateStatusByStatusList(targetStatus, sourceStatusList);
    }

    /**
     * 更新下载进度
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Integer taskId, long currentBytes, long totalBytes) {
        VideoDownloadTask task = this.getById(taskId);
        if (task != null) {
            task.setCurrentBytes(currentBytes);
            task.setTotalBytes(totalBytes);
            this.updateById(task);
        }
    }

    /**
     * 将任务标记为失败并记录错误信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void failTaskWithMessage(VideoDownloadTask videoDownloadTask, String errorMessage) {
        Integer id = videoDownloadTask.getId();
        VideoDownloadTask task = this.getById(id);
        if (task == null) {
            log.warn("failTaskWithMessage: 任务不存在 id={}", id);
            return;
        }
        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        if (Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED).contains(taskStatus)) {
            return;
        }
        task.setStatus(TaskStatus.FAILED.getCode());
        task.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                ? errorMessage.substring(0, 500) : errorMessage);
        this.updateById(task);
        log.info("任务失败 id:{} videoCode:{} error:{}", id, task.getVideoCode(), errorMessage);
    }
}
